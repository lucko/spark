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

import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class ForgeClientCommandSender extends AbstractCommandSender<CommandSourceStack> {
    public ForgeClientCommandSender(CommandSourceStack source) {
        super(source);
    }

    @Override
    public String getName() {
        return this.delegate.getTextName();
    }

    @Override
    public UUID getUniqueId() {
        Entity entity = this.delegate.getEntity();
        if (entity instanceof LocalPlayer player) {
            return player.getUUID();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        net.minecraft.network.chat.Component component = ComponentSerialization.CODEC.decode(
                RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE),
                GsonComponentSerializer.gson().serializeToTree(message)
        ).getOrThrow(JsonParseException::new).getFirst();
        super.delegate.sendSystemMessage(component);
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    @Override
    protected Object getObjectForComparison() {
        return this.delegate.getEntity();
    }
}
