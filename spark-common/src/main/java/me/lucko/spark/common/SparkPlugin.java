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

package me.lucko.spark.common;

import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.TickCounter;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Spark plugin interface
 */
public interface SparkPlugin {

    /**
     * Gets the version of the plugin.
     *
     * @return the version
     */
    String getVersion();

    /**
     * Gets the plugins storage/configuration directory.
     *
     * @return the plugin directory
     */
    Path getPluginDirectory();

    /**
     * Gets the name used for the plugin command.
     *
     * @return the plugin command name
     */
    String getCommandName();

    /**
     * Gets a {@link Stream} of the {@link CommandSender}s on the platform with the given
     * permission.
     *
     * @param permission the permission
     * @return the stream of command senders
     */
    Stream<? extends CommandSender> getSendersWithPermission(String permission);

    /**
     * Executes the given {@link Runnable} asynchronously using the plugins scheduler.
     *
     * @param task the task
     */
    void executeAsync(Runnable task);

    /**
     * Gets the default {@link ThreadDumper} to be used by the plugin.
     *
     * @return the default thread dumper
     */
    default ThreadDumper getDefaultThreadDumper() {
        return ThreadDumper.ALL;
    }

    /**
     * Creates a tick counter for the platform, if supported.
     *
     * <p>Returns {@code null} if the platform does not have "ticks"</p>
     *
     * @return a new tick counter
     */
    default TickCounter createTickCounter() {
        return null;
    }

}
