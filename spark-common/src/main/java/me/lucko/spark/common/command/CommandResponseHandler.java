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

package me.lucko.spark.common.command;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public class CommandResponseHandler {

    /** The prefix used in all messages "&8[&e&l⚡&8] &7" */
    private static final TextComponent PREFIX = text()
            .color(GRAY)
            .append(text("[", DARK_GRAY))
            .append(text("⚡", YELLOW, BOLD))
            .append(text("]", DARK_GRAY))
            .append(text(" "))
            .build();

    private final SparkPlatform platform;
    private final CommandSender sender;

    public CommandResponseHandler(SparkPlatform platform, CommandSender sender) {
        this.platform = platform;
        this.sender = sender;
    }

    public CommandSender sender() {
        return this.sender;
    }

    public void allSenders(Consumer<? super CommandSender> action) {
        Set<CommandSender> senders = this.platform.getPlugin().getSendersWithPermission("spark").collect(Collectors.toSet());
        senders.add(this.sender);
        senders.forEach(action);
    }

    public void reply(Component message) {
        this.sender.sendMessage(message);
    }

    public void broadcast(Component message) {
        allSenders(sender -> sender.sendMessage(message));
    }

    public void replyPrefixed(Component message) {
        reply(applyPrefix(message));
    }

    public void broadcastPrefixed(Component message) {
        broadcast(applyPrefix(message));
    }

    public static Component applyPrefix(Component message) {
        return PREFIX.append(message);
    }


}
