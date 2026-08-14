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

package me.lucko.spark.common.sampler.window;

import me.lucko.spark.common.sampler.aggregator.DataAggregator;
import me.lucko.spark.common.util.TimeUtil;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntPredicate;

public enum ProfilingWindowUtils {
    ;

    /**
     * The size/duration of a profiling window in seconds.
     * (1 window = 1 minute)
     */
    public static final int WINDOW_SIZE_SECONDS = 60;
    public static final int WINDOW_SIZE_MILLIS = WINDOW_SIZE_SECONDS * 1000;

    /**
     * The number of windows to record in continuous profiling before data is dropped.
     * (60 windows * 1 minute = 1 hour of profiling data)
     */
    public static final int HISTORY_SIZE = Integer.getInteger("spark.continuousProfilingHistorySize", 60);

    /**
     * The number of milliseconds to offset the profiling window by.
     * This is used to ensure that multiple servers don't all start their profiling windows at the same time.
     */
    public static final int WINDOW_ADJUSTMENT_MILLIS = ThreadLocalRandom.current().nextInt(WINDOW_SIZE_MILLIS) - (WINDOW_SIZE_MILLIS / 2);

    /**
     * Gets the profiling window for the given monotonic time in unix-millis
     *
     * <p>Window boundaries are not aligned to the minute, but are instead offset by a random amount
     * to avoid multiple servers all starting their profiling windows at the same time.</p>
     *
     * @param time the time in milliseconds
     * @return the window
     * @see TimeUtil#monotonicCurrentTimeMillis() for getting the current monotonic time
     */
    public static int monotonicTimeToWindow(long time) {
        return (int) ((time + WINDOW_ADJUSTMENT_MILLIS) / WINDOW_SIZE_MILLIS);
    }

    /**
     * Gets the window at the current time.
     *
     * @return the window
     */
    public static int windowNow() {
        return monotonicTimeToWindow(TimeUtil.monotonicCurrentTimeMillis());
    }

    /**
     * Gets a prune predicate that can be passed to {@link DataAggregator#pruneData(IntPredicate)}.
     *
     * @return the prune predicate - returns true for windows that should be pruned
     */
    public static IntPredicate keepHistoryBefore(int currentWindow) {
        // windows that were earlier than (currentWindow minus history size) should be pruned
        return window -> window < (currentWindow - HISTORY_SIZE);
    }
}
