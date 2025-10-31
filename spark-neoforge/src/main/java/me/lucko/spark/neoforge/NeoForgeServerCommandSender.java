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

package me.lucko.spark.neoforge;

import me.lucko.spark.minecraft.sender.MinecraftServerCommandSender;
import me.lucko.spark.neoforge.plugin.NeoForgeServerSparkPlugin;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;

public class NeoForgeServerCommandSender extends MinecraftServerCommandSender {
    private final NeoForgeServerSparkPlugin plugin;

    public NeoForgeServerCommandSender(CommandSourceStack commandSource, NeoForgeServerSparkPlugin plugin) {
        super(commandSource);
        this.plugin = plugin;
    }

    @Override
    public boolean hasPermission(String permission) {
        ServerPlayer player = this.delegate.getPlayer();
        if (player != null) {
            return PermissionAPI.getPermission(player, this.plugin.getPermissionNode(permission));
        } else {
            return true;
        }
    }
}
