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

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import me.lucko.spark.neoforge.plugin.NeoForgeServerSparkPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;

import java.util.Objects;
import java.util.UUID;

public class NeoForgeServerCommandSender extends AbstractCommandSender<CommandSourceStack> {
    private final NeoForgeServerSparkPlugin plugin;

    public NeoForgeServerCommandSender(CommandSourceStack commandSource, NeoForgeServerSparkPlugin plugin) {
        super(commandSource);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        String name = this.delegate.getTextName();
        if (this.delegate.getEntity() != null && name.equals("Server")) {
            return "Console";
        }
        return name;
    }

    @Override
    public UUID getUniqueId() {
        Entity entity = this.delegate.getEntity();
        return entity != null ? entity.getUUID() : null;
    }

    @Override
    public void sendMessage(Component message) {
        MutableComponent component = Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(message), RegistryAccess.EMPTY);
        Objects.requireNonNull(component, "component");
        super.delegate.sendSystemMessage(component);
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.plugin.hasPermission(this.delegate, permission);
    }

    @Override
    protected Object getObjectForComparison() {
        return this.delegate.getEntity();
    }
}
