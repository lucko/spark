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

package me.lucko.spark.forge;

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import me.lucko.spark.forge.plugin.Forge1710SparkPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeHooks;

import java.util.UUID;

public class Forge1710CommandSender extends AbstractCommandSender<ICommandSender> {
    private final Forge1710SparkPlugin plugin;

    public Forge1710CommandSender(ICommandSender source, Forge1710SparkPlugin plugin) {
        super(source);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        if (super.delegate instanceof EntityPlayer) {
            return ((EntityPlayer) super.delegate).getGameProfile().getName();
        } else if (super.delegate instanceof MinecraftServer) {
            return "Console";
        } else if (super.delegate instanceof RConConsoleSource) {
            return "RCON Console";
        } else {
            return "unknown:" + super.delegate.getClass().getSimpleName();
        }
    }

    @Override
    public UUID getUniqueId() {
        if (super.delegate instanceof EntityPlayer) {
            return ((EntityPlayer) super.delegate).getUniqueID();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        /*
         * Due to limitations in 1.7.10, messages with \n render incorrectly on the client.
         * To work around this, we convert the message to a string first, split it by newline,
         * and send each line individually.
         * 
         * This adds a performance penalty, but avoids any weirdness with this old client.
         */
        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
                .character(LegacyComponentSerializer.SECTION_CHAR)
                .extractUrls()
                .build();
        String output = serializer.serialize(message);
        for(String line : output.split("\n")) {
            Component deserialized = serializer.deserialize(line);
            IChatComponent mcComponent = IChatComponent.Serializer.func_150699_a(GsonComponentSerializer.gson().serialize(deserialized));
            super.delegate.addChatMessage(mcComponent);
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.plugin.hasPermission(super.delegate, permission);
    }
}
