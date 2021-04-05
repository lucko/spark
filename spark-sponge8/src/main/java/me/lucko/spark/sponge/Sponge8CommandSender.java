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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.service.permission.Subject;


import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Sponge8CommandSender extends AbstractCommandSender<Audience> {
    private final Subject subject;

    public Sponge8CommandSender(Subject subject, Audience audience) {
        super(audience);
        this.subject = subject;
    }

    @Override
    public String getName() {
        return subject.friendlyIdentifier().orElse(subject.identifier());
    }

    @Override
    public UUID getUniqueId() {
        try {
            return UUID.fromString(subject.identifier());
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(subject.identifier().getBytes(UTF_8));
        }
    }

    @Override
    public void sendMessage(Component message) {
        delegate.sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return subject.hasPermission(permission);
    }
}
