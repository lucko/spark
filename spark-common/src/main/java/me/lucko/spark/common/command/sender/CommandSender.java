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

package me.lucko.spark.common.command.sender;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.lucko.spark.proto.SparkProtos.CommandSenderData;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public interface CommandSender {

    String getName();

    UUID getUniqueId();

    void sendMessage(Component message);

    boolean hasPermission(String permission);

    default Data toData() {
        return new Data(getName(), getUniqueId());
    }

    final class Data {
        private final String name;
        private final UUID uniqueId;

        public Data(String name, UUID uniqueId) {
            this.name = name;
            this.uniqueId = uniqueId;
        }

        public String getName() {
            return this.name;
        }

        public UUID getUniqueId() {
            return this.uniqueId;
        }

        public boolean isPlayer() {
            return this.uniqueId != null;
        }

        public JsonObject serialize() {
            JsonObject user = new JsonObject();
            user.add("type", new JsonPrimitive(isPlayer() ? "player" : "other"));
            user.add("name", new JsonPrimitive(this.name));
            if (this.uniqueId != null) {
                user.add("uniqueId", new JsonPrimitive(this.uniqueId.toString()));
            }
            return user;
        }

        public CommandSenderData toProto() {
            CommandSenderData.Builder proto = CommandSenderData.newBuilder()
                    .setType(isPlayer() ? CommandSenderData.Type.PLAYER : CommandSenderData.Type.OTHER)
                    .setName(this.name);

            if (this.uniqueId != null) {
                proto.setUniqueId(this.uniqueId.toString());
            }

            return proto.build();
        }

        public static CommandSender.Data deserialize(JsonElement element) {
            JsonObject userObject = element.getAsJsonObject();
            String user = userObject.get("name").getAsJsonPrimitive().getAsString();
            UUID uuid;
            if (userObject.has("uniqueId")) {
                uuid = UUID.fromString(userObject.get("uniqueId").getAsJsonPrimitive().getAsString());
            } else {
                uuid = null;
            }
            return new CommandSender.Data(user, uuid);
        }
    }

}
