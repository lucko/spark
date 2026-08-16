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

package me.lucko.spark.common.platform;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.monitor.Metrics;
import me.lucko.spark.common.monitor.MonitoringExecutor;
import me.lucko.spark.common.platform.world.AsyncWorldInfoProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;

import java.util.concurrent.ScheduledFuture;

public class WorldMetricsCollector implements Runnable, AutoCloseable {
    private final AsyncWorldInfoProvider infoProvider;
    private ScheduledFuture<?> task;

    public WorldMetricsCollector(SparkPlatform platform) {
        WorldInfoProvider worldInfoProvider = platform.getPlugin().createWorldInfoProvider();
        this.infoProvider = worldInfoProvider == WorldInfoProvider.NO_OP ? null : new AsyncWorldInfoProvider(platform, worldInfoProvider);
    }

    public void start() {
        if (this.infoProvider == null) {
            return;
        }
        this.task = MonitoringExecutor.scheduleAtFixedRateMillis(this, Metrics.INTERVAL_MILLIS);
    }

    @Override
    public void run() {
        WorldInfoProvider.CountsResult counts = this.infoProvider.getCounts();
        if (counts != null) {
            Metrics.WORLD_INFO.record(counts);
        }
    }

    @Override
    public void close() {
        if (this.task != null) {
            this.task.cancel(false);
        }
    }
}
