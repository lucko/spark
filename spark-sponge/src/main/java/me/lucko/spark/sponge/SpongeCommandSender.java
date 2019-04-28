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

import me.lucko.spark.common.CommandSender;
import net.kyori.text.Component;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import org.spongepowered.api.command.CommandSource;

public class SpongeCommandSender implements CommandSender {
    private final CommandSource source;

    public SpongeCommandSender(CommandSource source) {
        this.source = source;
    }

    @Override
    public String getName() {
        return this.source.getName();
    }

    @Override
    public void sendMessage(Component message) {
        TextAdapter.sendComponent(this.source, message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.source.hasPermission(permission);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpongeCommandSender that = (SpongeCommandSender) o;
        return this.source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return this.source.hashCode();
    }
}
