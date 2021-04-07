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

package me.lucko.spark.api.gc;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Statistics about a garbage collector.
 *
 * <p>All time durations are measured in milliseconds.</p>
 */
public interface GarbageCollector {

    /**
     * Gets the name of the garbage collector.
     *
     * @return the name
     */
    @NonNull String name();

    /**
     * Gets the total number of collections performed.
     *
     * @return the total number of collections
     */
    long totalCollections();

    /**
     * Gets the total amount of time spent performing collections.
     *
     * <p>Measured in milliseconds.</p>
     *
     * @return the total time spent collecting
     */
    long totalTime();

    /**
     * Gets the average amount of time spent performing each collection.
     *
     * <p>Measured in milliseconds.</p>
     *
     * @return the average collection time
     */
    double avgTime();

    /**
     * Gets the average frequency at which collections are performed.
     *
     * <p>Measured in milliseconds.</p>
     *
     * @return the average frequency of collections
     */
    long avgFrequency();

}
