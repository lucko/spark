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

package me.lucko.spark.minecraft.sender;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public abstract class MinecraftServerCommandSender extends MinecraftCommandSender {
    public MinecraftServerCommandSender(CommandSourceStack source) {
        super(source);
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
    protected Object getObjectForComparison() {
        UUID uniqueId = getUniqueId();
        if (uniqueId != null) {
            return uniqueId;
        }
        Entity entity = this.delegate.getEntity();
        if (entity != null) {
            return entity;
        }
        return getName();
    }
}
