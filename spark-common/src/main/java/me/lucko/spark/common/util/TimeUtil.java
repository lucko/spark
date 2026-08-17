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

package me.lucko.spark.common.util;

/**
 * Utility class for time-related operations.
 *
 * <p>Provides a monotonic timestamp that is immune to system clock adjustments,
 * making it suitable for measuring elapsed time and avoiding issues with backwards
 * time jumps caused by NTP synchronization or manual clock changes.</p>
 */
public enum TimeUtil {
    ;

    private static final long REFERENCE_NANOS = System.nanoTime();
    private static final long REFERENCE_MILLIS = System.currentTimeMillis();

    /**
     * Returns the current time in milliseconds based on a monotonic clock.
     *
     * <p>This method provides a timestamp that increases monotonically and is not
     * affected by system clock adjustments. It combines {@link System#currentTimeMillis()}
     * for wall-clock time with {@link System#nanoTime()} for monotonic progression.</p>
     *
     * <p>The returned value represents wall-clock time (similar to
     * {@code System.currentTimeMillis()}), but is guaranteed to always move forward
     * at a steady rate, even if the system clock is adjusted backwards.</p>
     *
     * @return the current time in milliseconds, based on a monotonic clock
     */
    public static long monotonicCurrentTimeMillis() {
        return REFERENCE_MILLIS + ((System.nanoTime() - REFERENCE_NANOS) / 1_000_000L);
    }
}
