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
import me.lucko.spark.common.platform.PlatformInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;

import java.lang.ref.WeakReference;
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
    private final CommandSender.Data senderData;
    private final WeakReference<CommandSender> sender;
    private String commandPrimaryAlias;

    public CommandResponseHandler(SparkPlatform platform, CommandSender sender) {
        this.platform = platform;
        this.senderData = sender.toData();
        this.sender = new WeakReference<>(sender);
    }

    public void setCommandPrimaryAlias(String commandPrimaryAlias) {
        this.commandPrimaryAlias = commandPrimaryAlias;
    }

    public CommandSender.Data senderData() {
        return this.senderData;
    }

    private void sendMessage(CommandSender sender, Component message) {
        if (sender == null) {
            return;
        }

        if (this.platform.getPlugin().getPlatformInfo().getType() == PlatformInfo.Type.CLIENT) {
            // send message on the client render thread
            this.platform.getPlugin().executeSync(() -> {
                sender.sendMessage(message);
            });
        } else {
            sender.sendMessage(message);
        }
    }

    public void allSenders(Consumer<? super CommandSender> action) {
        if (this.commandPrimaryAlias == null) {
            throw new IllegalStateException("Command alias has not been set!");
        }

        Set<CommandSender> senders = this.platform.getPlugin().getCommandSenders()
                .filter(s -> s.hasPermission("spark") || s.hasPermission("spark." + this.commandPrimaryAlias))
                .collect(Collectors.toSet());

        CommandSender sender = this.sender.get();
        if (sender != null) {
            senders.add(sender);
        }

        senders.forEach(action);
    }

    public void reply(Component message) {
        sendMessage(this.sender.get(), message);
    }

    public void reply(Iterable<Component> message) {
        Component joinedMsg = Component.join(JoinConfiguration.separator(Component.newline()), message);
        reply(joinedMsg);
    }

    public void broadcast(Component message) {
        if (this.platform.shouldBroadcastResponse()) {
            allSenders(sender -> sendMessage(sender, message));
        } else {
            reply(message);
        }
    }

    public void broadcast(Iterable<Component> message) {
        if (this.platform.shouldBroadcastResponse()) {
            Component joinedMsg = Component.join(JoinConfiguration.separator(Component.newline()), message);
            allSenders(sender -> sendMessage(sender, joinedMsg));
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
