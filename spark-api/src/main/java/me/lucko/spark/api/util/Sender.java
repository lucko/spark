/*
 * This file is part of spark, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.spark.api.util;

import me.lucko.spark.proto.SparkProtos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a sender used for online uploading of data.
 */
public class Sender {
    public final String name;
    /**
     * The UUID of the sender. May be {@code null} if it wasn't sent by a player.
     */
    @Nullable
    public final UUID uuid;

    public Sender(String name, @Nullable UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }
    public Sender(String name) {
        this(name, null);
    }

    /**
     * Checks if this sender is a player.
     *
     * @return if this sender is a player
     */
    public boolean isPlayer() {
        return uuid != null;
    }

    public SparkProtos.CommandSenderMetadata toProto() {
        SparkProtos.CommandSenderMetadata.Builder proto = SparkProtos.CommandSenderMetadata.newBuilder()
                .setType(isPlayer() ? SparkProtos.CommandSenderMetadata.Type.PLAYER : SparkProtos.CommandSenderMetadata.Type.OTHER)
                .setName(this.name);

        if (this.uuid != null) {
            proto.setUniqueId(this.uuid.toString());
        }

        return proto.build();
    }
}
