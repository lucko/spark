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

package me.lucko.spark.common.monitor.memory;

import me.lucko.spark.common.monitor.Metrics;
import me.lucko.spark.common.monitor.MonitoringExecutor;
import me.lucko.spark.common.util.TimeUtil;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Monitors the /process memory usage.
 */
public enum MemoryMonitor {
    ;

    /** The MemoryMXBean instance */
    private static final MemoryMXBean BEAN = ManagementFactory.getMemoryMXBean();

    static {
        MonitoringExecutor.scheduleAtFixedRateMillis(new PollingTask(), Metrics.INTERVAL_MILLIS);
    }

    /**
     * Ensures that the static initializer has been called.
     */
    @SuppressWarnings("EmptyMethod")
    public static void ensureMonitoring() {
        // intentionally empty
    }

    /**
     * Task to poll memory usage.
     */
    private static final class PollingTask implements Runnable {

        @Override
        public void run() {
            long timeMillis = TimeUtil.monotonicCurrentTimeMillis();
            Metrics.MEMORY_USAGE_HEAP.record(timeMillis, BEAN.getHeapMemoryUsage());
            Metrics.MEMORY_USAGE_NON_HEAP.record(timeMillis, BEAN.getNonHeapMemoryUsage());
        }
    }

}
