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

import me.lucko.spark.common.CommandSender;
import me.lucko.spark.forge.plugin.ForgeSparkPlugin;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.rcon.IServer;
import net.minecraft.util.text.ITextComponent;

import java.util.UUID;

public class ForgeCommandSender implements CommandSender {
    private final ICommandSource sender;
    private final ForgeSparkPlugin plugin;

    public ForgeCommandSender(ICommandSource sender, ForgeSparkPlugin plugin) {
        this.sender = sender;
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        if (this.sender instanceof PlayerEntity) {
            return ((PlayerEntity) this.sender).getGameProfile().getName();
        } else if (this.sender instanceof IServer) {
            return "Console";
        } else {
            return "unknown:" + this.sender.getClass().getSimpleName();
        }
    }

    @Override
    public UUID getUniqueId() {
        if (this.sender instanceof PlayerEntity) {
            return ((PlayerEntity) this.sender).getUniqueID();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        ITextComponent component = ITextComponent.Serializer.fromJson(GsonComponentSerializer.INSTANCE.serialize(message));
        this.sender.sendMessage(component);
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.plugin.hasPermission(this.sender, permission);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForgeCommandSender that = (ForgeCommandSender) o;
        return this.sender.equals(that.sender);
    }

    @Override
    public int hashCode() {
        return this.sender.hashCode();
    }
}
