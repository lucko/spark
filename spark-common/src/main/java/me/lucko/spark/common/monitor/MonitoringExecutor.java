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

package me.lucko.spark.common.monitor;

import me.lucko.spark.common.util.SparkScheduledThreadPoolExecutor;
import me.lucko.spark.common.util.SparkThreadFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public enum MonitoringExecutor {
    ;

    /** The executor used to monitor & calculate rolling averages. */
    public static final ScheduledExecutorService INSTANCE = new SparkScheduledThreadPoolExecutor(4, new SparkThreadFactory("spark-monitoring", true));

    public static ScheduledFuture<?> scheduleAtFixedRateMillis(Runnable command, long periodMillis) {
        // schedule the task with a random initial delay to avoid all fixed rate tasks running at the same time
        long delay = ThreadLocalRandom.current().nextLong(Math.min(periodMillis, 10_000L));
        return INSTANCE.scheduleAtFixedRate(command, delay, periodMillis, MILLISECONDS);
    }

}
