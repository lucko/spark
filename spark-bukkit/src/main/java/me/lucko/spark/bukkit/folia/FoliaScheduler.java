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

package me.lucko.spark.bukkit.folia;

import me.lucko.spark.bukkit.BukkitSparkPlugin;
import me.lucko.spark.bukkit.BukkitSparkScheduler;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;

/**
 * Uses the {@link AsyncScheduler} for async operations,
 * and the {@link GlobalRegionScheduler} for sync operations.
 */
public final class FoliaScheduler implements BukkitSparkScheduler {
    private final BukkitSparkPlugin plugin;

    public FoliaScheduler(BukkitSparkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void executeAsync(Runnable task) {
        this.plugin.getServer().getAsyncScheduler().runNow(this.plugin, t -> task.run());
    }

    @Override
    public void executeSync(Runnable task) {
        this.plugin.getServer().getGlobalRegionScheduler().execute(this.plugin, task);
    }
}
