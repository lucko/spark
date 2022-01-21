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

package me.lucko.spark.common.platform;

import me.lucko.spark.proto.SparkProtos.PlatformMetadata;

public interface PlatformInfo {

    int DATA_VERSION = 2;

    Type getType();

    String getName();

    String getVersion();

    String getMinecraftVersion();

    default int getSparkVersion() {
        // does not necessarily correspond to the plugin/mod version
        return DATA_VERSION;
    }

    default OnlineMode getOnlineMode() {
        return OnlineMode.UNKNOWN;
    }

    default Data toData() {
        return new Data(getType(), getName(), getVersion(), getMinecraftVersion(), getSparkVersion(), getOnlineMode());
    }

    enum Type {
        SERVER(PlatformMetadata.Type.SERVER),
        CLIENT(PlatformMetadata.Type.CLIENT),
        PROXY(PlatformMetadata.Type.PROXY);

        private final PlatformMetadata.Type type;

        Type(PlatformMetadata.Type type) {
            this.type = type;
        }

        public PlatformMetadata.Type toProto() {
            return this.type;
        }
    }

    enum OnlineMode {
        UNKNOWN(PlatformMetadata.OnlineMode.UNKNOWN),
        ONLINE(PlatformMetadata.OnlineMode.ONLINE),
        OFFLINE(PlatformMetadata.OnlineMode.OFFLINE),
        BUNGEE(PlatformMetadata.OnlineMode.BUNGEE),
        VELOCITY(PlatformMetadata.OnlineMode.VELOCITY);

        private final PlatformMetadata.OnlineMode onlineMode;

        OnlineMode(PlatformMetadata.OnlineMode onlineMode) {
            this.onlineMode = onlineMode;
        }

        public PlatformMetadata.OnlineMode toProto() {
            return this.onlineMode;
        }
    }

    final class Data {
        private final Type type;
        private final String name;
        private final String version;
        private final String minecraftVersion;
        private final int sparkVersion;
        private final OnlineMode onlineMode;

        public Data(Type type, String name, String version, String minecraftVersion, int sparkVersion, OnlineMode onlineMode) {
            this.type = type;
            this.name = name;
            this.version = version;
            this.minecraftVersion = minecraftVersion;
            this.sparkVersion = sparkVersion;
            this.onlineMode = onlineMode;
        }

        public Type getType() {
            return this.type;
        }

        public String getName() {
            return this.name;
        }

        public String getVersion() {
            return this.version;
        }

        public String getMinecraftVersion() {
            return this.minecraftVersion;
        }

        public int getSparkVersion() {
            return this.sparkVersion;
        }

        public OnlineMode getOnlineMode() {
            return this.onlineMode;
        }

        public PlatformMetadata toProto() {
            PlatformMetadata.Builder proto = PlatformMetadata.newBuilder()
                    .setType(this.type.toProto())
                    .setName(this.name)
                    .setVersion(this.version)
                    .setSparkVersion(this.sparkVersion)
                    .setOnlineMode(this.onlineMode.toProto());

            if (this.minecraftVersion != null) {
                proto.setMinecraftVersion(this.minecraftVersion);
            }

            return proto.build();
        }
    }
}
