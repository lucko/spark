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
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

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
    private String commandPrimaryAlias;

    public CommandResponseHandler(SparkPlatform platform, CommandSender sender) {
        this.platform = platform;
        this.sender = sender;
    }

    public void setCommandPrimaryAlias(String commandPrimaryAlias) {
        this.commandPrimaryAlias = commandPrimaryAlias;
    }

    public CommandSender sender() {
        return this.sender;
    }

    public void allSenders(Consumer<? super CommandSender> action) {
        if (this.commandPrimaryAlias == null) {
            throw new IllegalStateException("Command alias has not been set!");
        }

        Set<CommandSender> senders = this.platform.getPlugin().getCommandSenders()
                .filter(s -> s.hasPermission("spark") || s.hasPermission("spark." + this.commandPrimaryAlias))
                .collect(Collectors.toSet());

        senders.add(this.sender);
        senders.forEach(action);
    }

    public void reply(Component message) {
        this.sender.sendMessage(message);
    }

    public void reply(Iterable<Component> message) {
        Component joinedMsg = Component.join(JoinConfiguration.separator(Component.newline()), message);
        this.sender.sendMessage(joinedMsg);
    }

    public void broadcast(Component message) {
        if (this.platform.shouldBroadcastResponse()) {
            allSenders(sender -> sender.sendMessage(message));
        } else {
            reply(message);
        }
    }

    public void broadcast(Iterable<Component> message) {
        if (this.platform.shouldBroadcastResponse()) {
            Component joinedMsg = Component.join(JoinConfiguration.separator(Component.newline()), message);
            allSenders(sender -> sender.sendMessage(joinedMsg));
        } else {
            reply(message);
        }
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
