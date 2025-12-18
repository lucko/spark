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

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.lucko.spark.minecraft.sender.MinecraftServerCommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class FabricServerCommandSender extends MinecraftServerCommandSender {
    private static final Permission PERMISSION_LEVEL_OWNERS = new Permission.HasCommandLevel(PermissionLevel.OWNERS);

    public FabricServerCommandSender(CommandSourceStack commandSource) {
        super(commandSource);
    }

    @Override
    public boolean hasPermission(String permission) {
        return Permissions.getPermissionValue(this.delegate, permission).orElseGet(() -> {
            ServerPlayer player = this.delegate.getPlayer();
            MinecraftServer server = this.delegate.getServer();
            if (player != null) {
                if (server != null && server.isSingleplayerOwner(player.nameAndId())) {
                    return true;
                }
                return player.permissions().hasPermission(PERMISSION_LEVEL_OWNERS);
            }
            return true;
        });
    }
}
