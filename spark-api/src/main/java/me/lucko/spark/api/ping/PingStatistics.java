/*
 * This file is part of spark, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.spark.api.ping;

import me.lucko.spark.api.Spark;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Utility interface used for ping analysis.
 *
 * @see Spark#ping()
 */
public interface PingStatistics {
    /**
     * Queries a summary of current player pings.
     *
     * @return a summary of current pings
     */
    @NotNull
    PingSummary getSummary();

    /**
     * Gets the ping average.
     *
     * @return the average
     */
    @NotNull
    DoubleAverageInfo getAverage();

    /**
     * Queries the ping of a given player.
     *
     * @param playerName the name of the player
     * @return the ping, if available
     */
    @Nullable
    PlayerPing query(String playerName);
}
