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

import me.lucko.spark.proto.SparkProtos.MemoryData;
import me.lucko.spark.proto.SparkProtos.PlatformData;

import java.lang.management.MemoryUsage;

public interface PlatformInfo {

    Type getType();

    String getName();

    String getVersion();

    String getMinecraftVersion();

    int getNCpus();

    MemoryUsage getHeapUsage();

    default Data toData() {
        return new Data(getType(), getName(), getVersion(), getMinecraftVersion(), getNCpus(), getHeapUsage());
    }

    enum Type {
        SERVER(PlatformData.Type.SERVER),
        CLIENT(PlatformData.Type.CLIENT),
        PROXY(PlatformData.Type.PROXY);

        private final PlatformData.Type type;

        Type(PlatformData.Type type) {
            this.type = type;
        }

        public PlatformData.Type toProto() {
            return this.type;
        }
    }

    final class Data {
        private final Type type;
        private final String name;
        private final String version;
        private final String minecraftVersion;
        private final int nCpus;
        private final MemoryUsage heapUsage;

        public Data(Type type, String name, String version, String minecraftVersion, int nCpus, MemoryUsage heapUsage) {
            this.type = type;
            this.name = name;
            this.version = version;
            this.minecraftVersion = minecraftVersion;
            this.nCpus = nCpus;
            this.heapUsage = heapUsage;
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

        public int getNCpus() {
            return this.nCpus;
        }

        public MemoryUsage getHeapUsage() {
            return this.heapUsage;
        }

        private static MemoryData toProto(MemoryUsage usage) {
            return MemoryData.newBuilder()
                    .setUsed(usage.getUsed())
                    .setCommitted(usage.getCommitted())
                    .setMax(usage.getMax())
                    .build();
        }

        public PlatformData toProto() {
            PlatformData.Builder proto = PlatformData.newBuilder()
                    .setType(this.type.toProto())
                    .setName(this.name)
                    .setVersion(this.version)
                    .setNCpus(this.nCpus)
                    .setHeapUsage(toProto(this.heapUsage));

            if (this.minecraftVersion != null) {
                proto.setMinecraftVersion(this.minecraftVersion);
            }

            return proto.build();
        }
    }
}
