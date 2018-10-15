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
import me.lucko.spark.common.http.Bytebin;
import me.lucko.spark.memory.HeapDump;

import java.io.IOException;
import java.util.function.Consumer;

public class HeapModule<S> implements CommandModule<S> {

    @Override
    public void registerCommands(Consumer<Command<S>> consumer) {
        consumer.accept(Command.<S>builder()
                .aliases("heap", "memory")
                .executor((platform, sender, arguments) -> {
                    platform.runAsync(() -> {
                        platform.sendPrefixedMessage("&7Creating a new heap dump, please wait...");

                        HeapDump heapDump;
                        try {
                            heapDump = HeapDump.createNew();
                        } catch (Exception e) {
                            platform.sendPrefixedMessage("&cAn error occurred whilst inspecting the heap.");
                            e.printStackTrace();
                            return;
                        }

                        byte[] output = heapDump.formCompressedDataPayload();
                        try {
                            String pasteId = Bytebin.postCompressedContent(output);
                            platform.sendPrefixedMessage("&bHeap dump output:");
                            platform.sendLink(SparkPlatform.VIEWER_URL + pasteId);
                        } catch (IOException e) {
                            platform.sendPrefixedMessage("&cAn error occurred whilst uploading the data.");
                            e.printStackTrace();
                        }
                    });
                })
                .tabCompleter((platform, sender, arguments) -> {
                    return null;
                })
                .build()
        );
    }

}
