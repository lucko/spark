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

import me.lucko.spark.api.Spark;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.platform.MetadataProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
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
     * Gets a {@link Stream} of the {@link CommandSender}s on the platform.
     *
     * @return the stream of command senders
     */
    Stream<? extends CommandSender> getCommandSenders();

    /**
     * Executes the given {@link Runnable} asynchronously using the plugins scheduler.
     *
     * @param task the task
     */
    void executeAsync(Runnable task);

    /**
     * Executes the given {@link Runnable} on the server/client main thread.
     *
     * @param task the task
     */
    default void executeSync(Runnable task) {
        throw new UnsupportedOperationException();
    }

    /**
     * Print to the plugin logger.
     *
     * @param level the log level
     * @param msg the message
     */
    void log(Level level, String msg);

    /**
     * Gets the default {@link ThreadDumper} to be used by the plugin.
     *
     * @return the default thread dumper
     */
    default ThreadDumper getDefaultThreadDumper() {
        return ThreadDumper.ALL;
    }

    /**
     * Creates a tick hook for the platform, if supported.
     *
     * <p>Returns {@code null} if the platform does not have "ticks"</p>
     *
     * @return a new tick hook
     */
    default TickHook createTickHook() {
        return null;
    }

    /**
     * Creates a tick reporter for the platform, if supported.
     *
     * <p>Returns {@code null} if the platform does not have "ticks"</p>
     *
     * @return a new tick reporter
     */
    default TickReporter createTickReporter() {
        return null;
    }

    /**
     * Creates tick statistics for the platform, if supported.
     *
     * <p>Spark is able to provide a default implementation for platforms that
     * provide a {@link TickHook} and {@link TickReporter}.</p>
     *
     * @return a new tick statistics instance
     */
    default TickStatistics createTickStatistics() {
        return null;
    }

    /**
     * Creates a class source lookup function.
     *
     * @return the class source lookup function
     */
    default ClassSourceLookup createClassSourceLookup() {
        return ClassSourceLookup.NO_OP;
    }

    /**
     * Gets a list of known sources (plugins/mods) on the platform.
     *
     * @return a list of sources
     */
    default Collection<SourceMetadata> getKnownSources() {
        return Collections.emptyList();
    }

    /**
     * Creates a player ping provider function.
     *
     * <p>Returns {@code null} if the platform does not support querying player pings</p>
     *
     * @return the player ping provider function
     */
    default PlayerPingProvider createPlayerPingProvider() {
        return null;
    }

    /**
     * Creates a server config provider.
     *
     * @return the server config provider function
     */
    default ServerConfigProvider createServerConfigProvider() {
        return null;
    }

    /**
     * Creates a metadata provider for the platform.
     *
     * @return the platform extra metadata provider
     */
    default MetadataProvider createExtraMetadataProvider() {
        return null;
    }

    /**
     * Creates a world info provider.
     *
     * @return the world info provider function
     */
    default WorldInfoProvider createWorldInfoProvider() {
        return WorldInfoProvider.NO_OP;
    }

    /**
     * Gets information for the platform.
     *
     * @return information about the platform
     */
    PlatformInfo getPlatformInfo();

    default void registerApi(Spark api) {

    }

}
