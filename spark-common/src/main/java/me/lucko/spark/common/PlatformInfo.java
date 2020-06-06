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

package me.lucko.spark.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.lucko.spark.proto.SparkProtos.PlatformData;

public interface PlatformInfo {

    Type getType();

    String getName();

    String getVersion();

    String getMinecraftVersion();

    default Data toData() {
        return new Data(getType(), getName(), getVersion(), getMinecraftVersion());
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
            return type;
        }

        public static Type fromProto(PlatformData.Type proto) {
            for (Type type : values()) {
                if (type.toProto() == proto) {
                    return type;
                }
            }

            return null;
        }

        public String getName() {
            return super.name().toLowerCase();
        }

        public static Type fromName(String name) {
            return valueOf(name.toUpperCase());
        }
    }

    final class Data {
        private final Type type;
        private final String name;
        private final String version;
        private final String minecraftVersion;

        public Data(Type type, String name, String version, String minecraftVersion) {
            this.type = type;
            this.name = name;
            this.version = version;
            this.minecraftVersion = minecraftVersion;
        }

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getMinecraftVersion() {
            return minecraftVersion;
        }

        // TODO: decide if necessary
        public JsonObject serialize() {
            JsonObject server = new JsonObject();
            server.add("type", new JsonPrimitive(this.type.toString().toLowerCase()));
            server.add("name", new JsonPrimitive(this.name));
            server.add("version", new JsonPrimitive(this.version));
            if (this.minecraftVersion != null) {
                server.add("minecraftVersion", new JsonPrimitive(this.minecraftVersion));
            }
            return server;
        }

        public PlatformData toProto() {
            PlatformData.Builder proto = PlatformData.newBuilder()
                    .setType(this.type.toProto())
                    .setName(this.name)
                    .setVersion(this.version);

            if (this.minecraftVersion != null) {
                proto.setMinecraftVersion(this.minecraftVersion);
            }

            return proto.build();
        }

        // TODO: decide if necessary
        public static PlatformInfo.Data deserialize(JsonElement element) {
            JsonObject serverObject = element.getAsJsonObject();
            Type type = Type.fromName(serverObject.get("type").getAsJsonPrimitive().getAsString());
            String name = serverObject.get("name").getAsJsonPrimitive().getAsString();
            String version = serverObject.get("version").getAsJsonPrimitive().getAsString();
            String minecraftVersion;
            if (serverObject.has("minecraftVersion")) {
                minecraftVersion = serverObject.get("minecraftVersion").getAsJsonPrimitive().getAsString();
            } else {
                minecraftVersion = null;
            }
            return new PlatformInfo.Data(type, name, version, minecraftVersion);
        }
    }
}
