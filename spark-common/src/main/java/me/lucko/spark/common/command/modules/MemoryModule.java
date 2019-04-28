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

import me.lucko.spark.common.ActivityLog;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.heapdump.HeapDump;
import me.lucko.spark.common.heapdump.HeapDumpSummary;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import okhttp3.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class MemoryModule implements CommandModule {
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("heapsummary")
                .argumentUsage("run-gc-before", null)
                .executor((platform, sender, resp, arguments) -> {
                    platform.getPlugin().runAsync(() -> {
                        if (arguments.boolFlag("run-gc-before")) {
                            resp.broadcastPrefixed(TextComponent.of("Running garbage collector..."));
                            System.gc();
                        }

                        resp.broadcastPrefixed(TextComponent.of("Creating a new heap dump summary, please wait..."));

                        HeapDumpSummary heapDump;
                        try {
                            heapDump = HeapDumpSummary.createNew();
                        } catch (Exception e) {
                            resp.broadcastPrefixed(TextComponent.of("An error occurred whilst inspecting the heap.", TextColor.RED));
                            e.printStackTrace();
                            return;
                        }

                        byte[] output = heapDump.formCompressedDataPayload();
                        try {
                            String key = SparkPlatform.BYTEBIN_CLIENT.postContent(output, JSON_TYPE, false).key();
                            String url = SparkPlatform.VIEWER_URL + key;

                            resp.broadcastPrefixed(TextComponent.of("Heap dump summmary output:", TextColor.GOLD));
                            resp.broadcast(TextComponent.builder(url)
                                    .color(TextColor.GRAY)
                                    .clickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                                    .build()
                            );

                            platform.getActivityLog().addToLog(new ActivityLog.Activity(sender.getName(), System.currentTimeMillis(), "Heap dump summary", url));
                        } catch (IOException e) {
                            resp.broadcastPrefixed(TextComponent.of("An error occurred whilst uploading the data.", TextColor.RED));
                            e.printStackTrace();
                        }
                    });
                })
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--run-gc-before"))
                .build()
        );

        consumer.accept(Command.builder()
                .aliases("heapdump")
                .argumentUsage("run-gc-before", null)
                .argumentUsage("include-non-live", null)
                .executor((platform, sender, resp, arguments) -> {
                    platform.getPlugin().runAsync(() -> {
                        Path pluginFolder = platform.getPlugin().getPluginFolder();
                        try {
                            Files.createDirectories(pluginFolder);
                        } catch (IOException e) {
                            // ignore
                        }

                        Path file = pluginFolder.resolve("heap-" + DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss").format(LocalDateTime.now()) + (HeapDump.isOpenJ9() ? ".phd" : ".hprof"));
                        boolean liveOnly = !arguments.boolFlag("include-non-live");

                        if (arguments.boolFlag("run-gc-before")) {
                            resp.broadcastPrefixed(TextComponent.of("Running garbage collector..."));
                            System.gc();
                        }

                        resp.broadcastPrefixed(TextComponent.of("Creating a new heap dump, please wait..."));

                        try {
                            HeapDump.dumpHeap(file, liveOnly);
                        } catch (Exception e) {
                            resp.broadcastPrefixed(TextComponent.of("An error occurred whilst creating a heap dump.", TextColor.RED));
                            e.printStackTrace();
                            return;
                        }

                        resp.broadcastPrefixed(TextComponent.of("Heap dump written to: " + file.toString(), TextColor.GOLD));
                    });
                })
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--run-gc-before", "--include-non-live"))
                .build()
        );
    }

}
