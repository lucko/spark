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

package me.lucko.spark.bukkit;

import org.bukkit.scheduler.BukkitScheduler;

/**
 * Interface for the server scheduler on Bukkit servers.
 */
public interface BukkitSparkScheduler {

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
    void executeSync(Runnable task);

    /**
     * Uses the {@link BukkitScheduler} for async and sync operations.
     */
    @SuppressWarnings("deprecation")
    final class Basic implements BukkitSparkScheduler {
        private final BukkitSparkPlugin plugin;

        public Basic(BukkitSparkPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void executeAsync(Runnable task) {
            this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, task);
        }

        @Override
        public void executeSync(Runnable task) {
            this.plugin.getServer().getScheduler().runTask(this.plugin, task);
        }
    }

}
