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
import me.lucko.spark.common.activitylog.ActivityLog.Activity;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.heapdump.HeapDump;
import me.lucko.spark.common.heapdump.HeapDumpSummary;
import me.lucko.spark.common.util.FormatUtil;
import net.kyori.adventure.text.event.ClickEvent;
import okhttp3.MediaType;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAOutputStream;
import org.tukaani.xz.XZOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.zip.GZIPOutputStream;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class HeapAnalysisModule implements CommandModule {
    private static final MediaType SPARK_HEAP_MEDIA_TYPE = MediaType.parse("application/x-spark-heap");

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("heapsummary")
                .argumentUsage("run-gc-before", null)
                .executor((platform, sender, resp, arguments) -> {
                    platform.getPlugin().executeAsync(() -> {
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

                        byte[] output = heapDump.formCompressedDataPayload(platform.getPlugin().getPlatformInfo(), sender);
                        try {
                            String key = SparkPlatform.BYTEBIN_CLIENT.postContent(output, SPARK_HEAP_MEDIA_TYPE, false).key();
                            String url = SparkPlatform.VIEWER_URL + key;

                            resp.broadcastPrefixed(text("Heap dump summmary output:", GOLD));
                            resp.broadcast(text()
                                    .content(url)
                                    .color(GRAY)
                                    .clickEvent(ClickEvent.openUrl(url))
                                    .build()
                            );

                            platform.getActivityLog().addToLog(Activity.urlActivity(sender, System.currentTimeMillis(), "Heap dump summary", url));
                        } catch (IOException e) {
                            resp.broadcastPrefixed(text("An error occurred whilst uploading the data.", RED));
                            e.printStackTrace();
                        }
                    });
                })
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--run-gc-before"))
                .build()
        );

        consumer.accept(Command.builder()
                .aliases("heapdump")
                .argumentUsage("compress", "type")
                .argumentUsage("run-gc-before", null)
                .argumentUsage("include-non-live", null)
                .executor((platform, sender, resp, arguments) -> {
                    platform.getPlugin().executeAsync(() -> {
                        Path pluginFolder = platform.getPlugin().getPluginDirectory();
                        try {
                            Files.createDirectories(pluginFolder);
                        } catch (IOException e) {
                            // ignore
                        }

                        Path file = pluginFolder.resolve("heap-" + DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss").format(LocalDateTime.now()) + (HeapDump.isOpenJ9() ? ".phd" : ".hprof"));
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


                        CompressionMethod compress = null;
                        Iterator<String> compressArgs = arguments.stringFlag("compress").iterator();
                        if (compressArgs.hasNext()) {
                            try {
                                compress = CompressionMethod.valueOf(compressArgs.next().toUpperCase());
                            } catch (IllegalArgumentException e) {
                                // ignore
                            }
                        }

                        if (compress != null) {
                            resp.broadcastPrefixed(text("Compressing heap dump, please wait..."));
                            try {
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

                                Path compressedFile = compress.compress(file, progressHandler);
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
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                })
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--compress", "--run-gc-before", "--include-non-live"))
                .build()
        );
    }

    public enum CompressionMethod {
        GZIP {
            @Override
            public Path compress(Path file, LongConsumer progressHandler) throws IOException {
                Path compressedFile = file.getParent().resolve(file.getFileName().toString() + ".gz");
                try (InputStream in = Files.newInputStream(file)) {
                    try (OutputStream out = Files.newOutputStream(compressedFile)) {
                        try (GZIPOutputStream compressionOut = new GZIPOutputStream(out, 1024 * 64)) {
                            copy(in, compressionOut, progressHandler);
                        }
                    }
                }
                return compressedFile;
            }
        },
        XZ {
            @Override
            public Path compress(Path file, LongConsumer progressHandler) throws IOException {
                Path compressedFile = file.getParent().resolve(file.getFileName().toString() + ".xz");
                try (InputStream in = Files.newInputStream(file)) {
                    try (OutputStream out = Files.newOutputStream(compressedFile)) {
                        try (XZOutputStream compressionOut = new XZOutputStream(out, new LZMA2Options())) {
                            copy(in, compressionOut, progressHandler);
                        }
                    }
                }
                return compressedFile;
            }
        },
        LZMA {
            @Override
            public Path compress(Path file, LongConsumer progressHandler) throws IOException {
                Path compressedFile = file.getParent().resolve(file.getFileName().toString() + ".lzma");
                try (InputStream in = Files.newInputStream(file)) {
                    try (OutputStream out = Files.newOutputStream(compressedFile)) {
                        try (LZMAOutputStream compressionOut = new LZMAOutputStream(out, new LZMA2Options(), true)) {
                            copy(in, compressionOut, progressHandler);
                        }
                    }
                }
                return compressedFile;
            }
        };

        public abstract Path compress(Path file, LongConsumer progressHandler) throws IOException;

        private static long copy(InputStream from, OutputStream to, LongConsumer progress) throws IOException {
            byte[] buf = new byte[1024 * 64];
            long total = 0;
            long iterations = 0;
            while (true) {
                int r = from.read(buf);
                if (r == -1) {
                    break;
                }
                to.write(buf, 0, r);
                total += r;

                // report progress every 5MB
                if (iterations++ % ((1024 / 64) * 5) == 0) {
                    progress.accept(total);
                }
            }
            return total;
        }
    }

}
