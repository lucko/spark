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

package me.lucko.spark.standalone;

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;

import java.util.UUID;

public class StandaloneCommandSender extends AbstractCommandSender<StandaloneCommandSender.Output> {
    public static final StandaloneCommandSender NO_OP = new StandaloneCommandSender(msg -> {});
    public static final StandaloneCommandSender SYSTEM_OUT = new StandaloneCommandSender(System.out::println);

    public StandaloneCommandSender(Output output) {
        super(output);
    }

    @Override
    public String getName() {
        return "Standalone";
    }

    @Override
    public UUID getUniqueId() {
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        this.delegate.sendMessage(ANSIComponentSerializer.ansi().serialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    public interface Output {
        void sendMessage(String message);
    }

}
