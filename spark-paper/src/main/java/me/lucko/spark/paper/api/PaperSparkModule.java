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

package me.lucko.spark.paper.api;

import me.lucko.spark.paper.PaperSparkPlugin;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.logging.Logger;

/**
 * Spark module for use as a library within the Paper server implementation.
 *
 * <p>Paper provides:</p>
 * <ul>
 *     <li>a {@link Server} instance</li>
 *     <li>a {@link Logger} instance</li>
 *     <li>a {@link PaperScheduler} instance</li>
 *     <li>a {@link PaperClassLookup} instance</li>
 * </ul>
 *
 * <p>Paper is expected to:</p>
 * <ul>
 *     <li>call {@link #enable()} to enable spark, either immediately or when the server has finished starting</li>
 *     <li>call {@link #disable()} to disable spark when the server is stopping</li>
 *     <li>call {@link #executeCommand(CommandSender, String[])} when the spark command is executed</li>
 *     <li>call {@link #tabComplete(CommandSender, String[])} when the spark command is tab completed</li>
 *     <li>call {@link #onServerTickStart()} at the start of each server tick</li>
 *     <li>call {@link #onServerTickEnd(double)} at the end of each server tick</li>
 * </ul>
 *
 * <p>This interface and the other interfaces in this package define the API between Paper and spark. All other classes
 * are subject to change, even between minor versions.</p>
 */
public interface PaperSparkModule {

    /**
     * Creates a new PaperSparkModule.
     *
     * @param compatibility the Paper/spark compatibility version
     * @param server the server
     * @param logger a logger that can be used by spark
     * @param scheduler the scheduler
     * @param classLookup a class lookup utility
     * @return a new PaperSparkModule
     */
    static PaperSparkModule create(Compatibility compatibility, Server server, Logger logger, PaperScheduler scheduler, PaperClassLookup classLookup) {
        return new PaperSparkPlugin(server, logger, scheduler, classLookup);
    }

    /**
     * Enables the spark module.
     */
    void enable();

    /**
     * Disables the spark module.
     */
    void disable();

    /**
     * Handles a command execution.
     *
     * @param sender the sender
     * @param args the command arguments
     */
    void executeCommand(CommandSender sender, String[] args);

    /**
     * Handles a tab completion request.
     *
     * @param sender the sender
     * @param args the command arguments
     * @return a list of completions
     */
    List<String> tabComplete(CommandSender sender, String[] args);

    /**
     * Called by Paper at the start of each server tick.
     */
    void onServerTickStart();

    /**
     * Called by Paper at the end of each server tick.
     *
     * @param duration the duration of the tick
     */
    void onServerTickEnd(double duration);

}
