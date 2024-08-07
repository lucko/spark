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

package me.lucko.spark.sponge;

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.UUID;

public class Sponge7CommandSender extends AbstractCommandSender<CommandSource> {
    public Sponge7CommandSender(CommandSource source) {
        super(source);
    }

    @Override
    public String getName() {
        return super.delegate.getName();
    }

    @Override
    public UUID getUniqueId() {
        if (super.delegate instanceof Player) {
            return ((Player) super.delegate).getUniqueId();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        super.delegate.sendMessage(TextSerializers.JSON.deserialize(GsonComponentSerializer.gson().serialize(message)));
    }

    @Override
    public boolean hasPermission(String permission) {
        return super.delegate.hasPermission(permission);
    }
}
