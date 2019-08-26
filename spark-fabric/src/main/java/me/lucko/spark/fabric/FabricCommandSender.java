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

package me.lucko.spark.fabric;

import me.lucko.spark.common.CommandSender;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.text.Text;

import java.util.UUID;

public class FabricCommandSender implements CommandSender {

    private final VanillaPermission permission;
    private final CommandOutput sender;

    public FabricCommandSender(VanillaPermission permission, CommandOutput sender) {
        this.permission = permission;
        this.sender = sender;
    }

    @Override
    public String getName() {
        if (this.sender instanceof PlayerEntity) {
            return ((PlayerEntity) this.sender).getGameProfile().getName();
        } else if (this.sender instanceof DedicatedServer) {
            return "Console";
        } else {
            return "unknown:" + this.sender.getClass().getSimpleName();
        }
    }

    @Override
    public UUID getUniqueId() {
        if (this.sender instanceof PlayerEntity) {
            return ((PlayerEntity) this.sender).getUuid();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        Text component = Text.Serializer.fromJson(GsonComponentSerializer.INSTANCE.serialize(message));
        this.sender.sendMessage(component);
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.permission.hasPermissionLevel(4); // Require /stop access, reasonable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FabricCommandSender that = (FabricCommandSender) o;
        return this.sender.equals(that.sender);
    }

    @Override
    public int hashCode() {
        return this.sender.hashCode();
    }

    public interface VanillaPermission {

        boolean hasPermissionLevel(int level);
    }
}
