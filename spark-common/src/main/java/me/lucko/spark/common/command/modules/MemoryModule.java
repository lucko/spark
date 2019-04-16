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
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.memory.HeapDump;
import me.lucko.spark.common.memory.HeapDumpSummary;
import okhttp3.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class MemoryModule<S> implements CommandModule<S> {
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Override
    public void registerCommands(Consumer<Command<S>> consumer) {
        consumer.accept(Command.<S>builder()
                .aliases("heapsummary")
                .argumentUsage("run-gc-before", null)
                .executor((platform, sender, resp, arguments) -> {
                    platform.getPlugin().runAsync(() -> {
                                    if (arguments.boolFlag("run-gc-before")) {
                                        resp.broadcastPrefixed("&7Running garbage collector...");
                                        System.gc();
                                    }

                                    resp.broadcastPrefixed("&7Creating a new heap dump summary, please wait...");

                                    HeapDumpSummary heapDump;
                                    try {
                                        heapDump = HeapDumpSummary.createNew();
                                    } catch (Exception e) {
                                        resp.broadcastPrefixed("&cAn error occurred whilst inspecting the heap.");
                                        e.printStackTrace();
                                        return;
                                    }

                                    byte[] output = heapDump.formCompressedDataPayload();
                                    try {
                                        String key = SparkPlatform.BYTEBIN_CLIENT.postContent(output, JSON_TYPE, false).key();
                                        resp.broadcastPrefixed("&bHeap dump summmary output:");
                                        resp.broadcastLink(SparkPlatform.VIEWER_URL + key);
                                    } catch (IOException e) {
                                        resp.broadcastPrefixed("&cAn error occurred whilst uploading the data.");
                                        e.printStackTrace();
                                    }
                                });
                })
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--run-gc-before"))
                .build()
        );

        consumer.accept(Command.<S>builder()
                .aliases("heapdump")
                .argumentUsage("run-gc-before", null)
                .argumentUsage("include-non-live", null)
                .executor((platform, sender, resp, arguments) -> {
                    // ignore
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
                                        resp.broadcastPrefixed("&7Running garbage collector...");
                                        System.gc();
                                    }

                                    resp.broadcastPrefixed("&7Creating a new heap dump, please wait...");

                                    try {
                                        HeapDump.dumpHeap(file, liveOnly);
                                    } catch (Exception e) {
                                        resp.broadcastPrefixed("&cAn error occurred whilst creating a heap dump.");
                                        e.printStackTrace();
                                        return;
                                    }

                                    resp.broadcastPrefixed("&bHeap dump written to: " + file.toString());
                                });
                })
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--run-gc-before", "--include-non-live"))
                .build()
        );
    }

}
