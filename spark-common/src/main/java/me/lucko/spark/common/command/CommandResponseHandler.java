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

import java.util.Set;
import java.util.function.Consumer;

public class CommandResponseHandler<S> {

    /** The prefix used in all messages */
    private static final String PREFIX = "&8[&e&l⚡&8] &7";

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

    public void reply(String message) {
        this.platform.getPlugin().sendMessage(this.sender, message);
    }

    public void broadcast(String message) {
        allSenders(sender -> this.platform.getPlugin().sendMessage(sender, message));
    }

    public void replyPrefixed(String message) {
        this.platform.getPlugin().sendMessage(this.sender, PREFIX + message);
    }

    public void broadcastPrefixed(String message) {
        allSenders(sender -> this.platform.getPlugin().sendMessage(sender, PREFIX + message));
    }

    public void replyLink(String link) {
        this.platform.getPlugin().sendLink(this.sender, link);
    }

    public void broadcastLink(String link) {
        allSenders(sender -> this.platform.getPlugin().sendLink(sender, link));
    }

}
