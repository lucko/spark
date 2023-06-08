/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.common.command.modules;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.activitylog.Activity;
import me.lucko.spark.common.command.Arguments;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.heapdump.HeapDump;
import me.lucko.spark.common.heapdump.HeapDumpSummary;
import me.lucko.spark.common.util.Compression;
import me.lucko.spark.common.util.FormatUtil;
import me.lucko.spark.common.util.MediaTypes;
import me.lucko.spark.proto.SparkHeapProtos;

import net.kyori.adventure.text.event.ClickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public class HeapAnalysisModule implements CommandModule {

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("heapsummary")
                .argumentUsage("save-to-file", null)
                .executor(HeapAnalysisModule::heapSummary)
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--save-to-file", "--run-gc-before"))
                .build()
        );

        consumer.accept(Command.builder()
                .aliases("heapdump")
                .argumentUsage("compress", "type")
                .executor(HeapAnalysisModule::heapDump)
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--compress", "--run-gc-before", "--include-non-live"))
                .build()
        );
    }

    private static void heapSummary(SparkPlatform platform, CommandSender sender, CommandResponseHandler resp, Arguments arguments) {
        if (arguments.boolFlag("run-gc-before")) {
            resp.broadcastPrefixed(text("Running garbage collector..."));
            System.gc();
        }

        resp.broadcastPrefixed(text("Creating a new heap dump summary, please wait..."));

        HeapDumpSummary heapDump;
        try {
            heapDump = HeapDumpSummary.createNew();
        } catch (Exception e) {
            resp.broadcastPrefixed(text("An error occurred whilst inspecting the heap.", RED));
            e.printStackTrace();
            return;
        }

        SparkHeapProtos.HeapData output = heapDump.toProto(platform, sender);

        boolean saveToFile = false;
        if (arguments.boolFlag("save-to-file")) {
            saveToFile = true;
        } else {
            try {
                String key = platform.getBytebinClient().postContent(output, MediaTypes.SPARK_HEAP_MEDIA_TYPE).key();
                String url = platform.getViewerUrl() + key;

                resp.broadcastPrefixed(text("Heap dump summmary output:", GOLD));
                resp.broadcast(text()
                        .content(url)
                        .color(GRAY)
                        .clickEvent(ClickEvent.openUrl(url))
                        .build()
                );

                platform.getActivityLog().addToLog(Activity.urlActivity(sender, System.currentTimeMillis(), "Heap dump summary", url));
            } catch (Exception e) {
                resp.broadcastPrefixed(text("An error occurred whilst uploading the data. Attempting to save to disk instead.", RED));
                e.printStackTrace();
                saveToFile = true;
            }
        }

        if (saveToFile) {
            Path file = platform.resolveSaveFile("heapsummary", "sparkheap");
            try {
                Files.write(file, output.toByteArray());

                resp.broadcastPrefixed(text()
                        .content("Heap dump summary written to: ")
                        .color(GOLD)
                        .append(text(file.toString(), GRAY))
                        .build()
                );
                resp.broadcastPrefixed(text("You can read the heap dump summary file using the viewer web-app - " + platform.getViewerUrl(), GRAY));

                platform.getActivityLog().addToLog(Activity.fileActivity(sender, System.currentTimeMillis(), "Heap dump summary", file.toString()));
            } catch (IOException e) {
                resp.broadcastPrefixed(text("An error occurred whilst saving the data.", RED));
                e.printStackTrace();
            }
        }

    }

    private static void heapDump(SparkPlatform platform, CommandSender sender, CommandResponseHandler resp, Arguments arguments) {
        Path file = platform.resolveSaveFile("heap", HeapDump.isOpenJ9() ? "phd" : "hprof");

        boolean liveOnly = !arguments.boolFlag("include-non-live");

        if (arguments.boolFlag("run-gc-before")) {
            resp.broadcastPrefixed(text("Running garbage collector..."));
            System.gc();
        }

        resp.broadcastPrefixed(text("Creating a new heap dump, please wait..."));

        try {
            HeapDump.dumpHeap(file, liveOnly);
        } catch (Exception e) {
            resp.broadcastPrefixed(text("An error occurred whilst creating a heap dump.", RED));
            e.printStackTrace();
            return;
        }

        resp.broadcastPrefixed(text()
                .content("Heap dump written to: ")
                .color(GOLD)
                .append(text(file.toString(), GRAY))
                .build()
        );
        platform.getActivityLog().addToLog(Activity.fileActivity(sender, System.currentTimeMillis(), "Heap dump", file.toString()));


        Compression compressionMethod = null;
        Iterator<String> compressArgs = arguments.stringFlag("compress").iterator();
        if (compressArgs.hasNext()) {
            try {
                compressionMethod = Compression.valueOf(compressArgs.next().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        if (compressionMethod != null) {
            try {
                heapDumpCompress(platform, resp, file, compressionMethod);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void heapDumpCompress(SparkPlatform platform, CommandResponseHandler resp, Path file, Compression method) throws IOException {
        resp.broadcastPrefixed(text("Compressing heap dump, please wait..."));

        long size = Files.size(file);
        AtomicLong lastReport = new AtomicLong(System.currentTimeMillis());

        LongConsumer progressHandler = progress -> {
            long timeSinceLastReport = System.currentTimeMillis() - lastReport.get();
            if (timeSinceLastReport > TimeUnit.SECONDS.toMillis(5)) {
                lastReport.set(System.currentTimeMillis());

                platform.getPlugin().executeAsync(() -> {
                    resp.broadcastPrefixed(text()
                            .color(GRAY)
                            .append(text("Compressed "))
                            .append(text(FormatUtil.formatBytes(progress), GOLD))
                            .append(text(" / "))
                            .append(text(FormatUtil.formatBytes(size), GOLD))
                            .append(text(" so far... ("))
                            .append(text(FormatUtil.percent(progress, size), GREEN))
                            .append(text(")"))
                            .build()
                    );
                });
            }
        };

        Path compressedFile = method.compress(file, progressHandler);
        long compressedSize = Files.size(compressedFile);

        resp.broadcastPrefixed(text()
                .color(GRAY)
                .append(text("Compression complete: "))
                .append(text(FormatUtil.formatBytes(size), GOLD))
                .append(text(" --> "))
                .append(text(FormatUtil.formatBytes(compressedSize), GOLD))
                .append(text(" ("))
                .append(text(FormatUtil.percent(compressedSize, size), GREEN))
                .append(text(")"))
                .build()
        );

        resp.broadcastPrefixed(text()
                .content("Compressed heap dump written to: ")
                .color(GOLD)
                .append(text(compressedFile.toString(), GRAY))
                .build()
        );
    }

}
