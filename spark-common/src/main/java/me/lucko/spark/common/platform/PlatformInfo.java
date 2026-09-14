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

    Type getType();

    String getName();

    String getBrand();

    String getVersion();

    String getMinecraftVersion();

    default Data toData(String sparkVersion, int sparkDataVersion) {
        return new Data(getType(), getName(), getBrand(), getVersion(), getMinecraftVersion(), sparkVersion, sparkDataVersion);
    }

    enum Type {
        SERVER(PlatformMetadata.Type.SERVER),
        CLIENT(PlatformMetadata.Type.CLIENT),
        PROXY(PlatformMetadata.Type.PROXY),
        APPLICATION(PlatformMetadata.Type.APPLICATION);

        private final PlatformMetadata.Type type;

        Type(PlatformMetadata.Type type) {
            this.type = type;
        }

        public PlatformMetadata.Type toProto() {
            return this.type;
        }
    }

    final class Data {
        private final Type type;
        private final String name;
        private final String brand;
        private final String version;
        private final String minecraftVersion;
        private final String sparkVersion;
        private final int sparkDataVersion;

        public Data(Type type, String name, String brand, String version, String minecraftVersion, String sparkVersion, int sparkDataVersion) {
            this.type = type;
            this.name = name;
            this.brand = brand;
            this.version = version;
            this.minecraftVersion = minecraftVersion;
            this.sparkVersion = sparkVersion;
            this.sparkDataVersion = sparkDataVersion;
        }

        public Type getType() {
            return this.type;
        }

        public String getName() {
            return this.name;
        }

        public String getBrand() {
            return this.brand;
        }

        public String getVersion() {
            return this.version;
        }

        public String getMinecraftVersion() {
            return this.minecraftVersion;
        }

        public String getSparkVersion() {
            return this.sparkVersion;
        }

        public int getSparkDataVersion() {
            return this.sparkDataVersion;
        }

        public PlatformMetadata toProto() {
            PlatformMetadata.Builder proto = PlatformMetadata.newBuilder()
                    .setType(this.type.toProto())
                    .setName(this.name)
                    .setBrand(this.brand)
                    .setVersion(this.version)
                    .setSparkVersion(this.sparkVersion)
                    .setSparkDataVersion(this.sparkDataVersion);

            if (this.minecraftVersion != null) {
                proto.setMinecraftVersion(this.minecraftVersion);
            }

            return proto.build();
        }
    }
}
