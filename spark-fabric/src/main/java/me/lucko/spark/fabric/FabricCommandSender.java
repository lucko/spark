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

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import me.lucko.spark.fabric.plugin.FabricSparkPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.rcon.RconCommandOutput;
import net.minecraft.text.Text;

import java.util.UUID;

public class FabricCommandSender extends AbstractCommandSender<CommandOutput> {
    private static final UUID NIL_UUID = new UUID(0, 0);

    private final FabricSparkPlugin plugin;

    public FabricCommandSender(CommandOutput commandOutput, FabricSparkPlugin plugin) {
        super(commandOutput);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        if (super.delegate instanceof PlayerEntity) {
            return ((PlayerEntity) super.delegate).getGameProfile().getName();
        } else if (super.delegate instanceof MinecraftServer) {
            return "Console";
        } else if (super.delegate instanceof RconCommandOutput) {
            return "RCON Console";
        } else {
            return "unknown:" + super.delegate.getClass().getSimpleName();
        }
    }

    @Override
    public UUID getUniqueId() {
        if (super.delegate instanceof PlayerEntity) {
            return ((PlayerEntity) super.delegate).getUuid();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        Text component = Text.Serializer.fromJson(GsonComponentSerializer.gson().serialize(message));
        super.delegate.sendSystemMessage(component, NIL_UUID);
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.plugin.hasPermission(super.delegate, permission);
    }
}
