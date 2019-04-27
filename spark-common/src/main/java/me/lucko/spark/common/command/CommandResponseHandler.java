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
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;

import java.util.Set;
import java.util.function.Consumer;

public class CommandResponseHandler<S> {

    /** The prefix used in all messages "&8[&e&l⚡&8] &7" */
    private static final TextComponent PREFIX = TextComponent.builder().color(TextColor.GRAY)
            .append(TextComponent.of("[", TextColor.DARK_GRAY))
            .append(TextComponent.builder("⚡").color(TextColor.YELLOW).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE).build())
            .append(TextComponent.of("]", TextColor.DARK_GRAY))
            .append(TextComponent.of(" "))
            .build();

    private final SparkPlatform<S> platform;
    private final S sender;

    public CommandResponseHandler(SparkPlatform<S> platform, S sender) {
        this.platform = platform;
        this.sender = sender;
    }

    public S sender() {
        return this.sender;
    }

    public void allSenders(Consumer<? super S> action) {
        Set<S> senders = this.platform.getPlugin().getSendersWithPermission("spark");
        senders.add(this.sender);
        senders.forEach(action);
    }

    public void reply(Component message) {
        this.platform.getPlugin().sendMessage(this.sender, message);
    }

    public void broadcast(Component message) {
        allSenders(sender -> this.platform.getPlugin().sendMessage(sender, message));
    }

    public void replyPrefixed(Component message) {
        reply(PREFIX.append(message));
    }

    public void broadcastPrefixed(Component message) {
        broadcast(PREFIX.append(message));
    }


}
