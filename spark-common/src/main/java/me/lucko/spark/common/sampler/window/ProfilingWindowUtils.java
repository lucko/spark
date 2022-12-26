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

import java.util.function.IntPredicate;

public enum ProfilingWindowUtils {
    ;

    /**
     * The size/duration of a profiling window in seconds.
     * (1 window = 1 minute)
     */
    public static final int WINDOW_SIZE_SECONDS = 60;

    /**
     * The number of windows to record in continuous profiling before data is dropped.
     * (60 windows * 1 minute = 1 hour of profiling data)
     */
    public static final int HISTORY_SIZE = Integer.getInteger("spark.continuousProfilingHistorySize", 60);

    /**
     * Gets the profiling window for the given time in unix-millis.
     *
     * @param time the time in milliseconds
     * @return the window
     */
    public static int unixMillisToWindow(long time) {
        return (int) (time / (WINDOW_SIZE_SECONDS * 1000L));
    }

    /**
     * Gets the window at the current time.
     *
     * @return the window
     */
    public static int windowNow() {
        return unixMillisToWindow(System.currentTimeMillis());
    }

    /**
     * Gets a prune predicate that can be passed to {@link DataAggregator#pruneData(IntPredicate)}.
     *
     * @return the prune predicate
     */
    public static IntPredicate keepHistoryBefore(int currentWindow) {
        // windows that were earlier than (currentWindow minus history size) should be pruned
        return window -> window < (currentWindow - HISTORY_SIZE);
    }
}
