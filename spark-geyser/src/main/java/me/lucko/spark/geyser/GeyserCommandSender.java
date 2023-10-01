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

package me.lucko.spark.geyser;

import me.lucko.spark.common.command.sender.AbstractCommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;

public class GeyserCommandSender extends AbstractCommandSender<CommandSource> {

    public GeyserCommandSender(CommandSource delegate) {
        super(delegate);
    }

    @Override
    public String getName() {
        return this.delegate.name();
    }

    @Override
    public UUID getUniqueId() {
        if (this.delegate instanceof GeyserSession) {
            return ((GeyserSession) this.delegate).javaUuid();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        this.delegate.sendMessage(LegacyComponentSerializer.legacySection().serialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.delegate.hasPermission(permission);
    }
}
