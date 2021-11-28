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

package me.lucko.spark.api;

import me.lucko.spark.api.gc.GarbageCollector;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;

import static me.lucko.spark.api.statistic.StatisticWindow.CpuUsage;
import static me.lucko.spark.api.statistic.StatisticWindow.MillisPerTick;
import static me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond;

/**
 * The spark API.
 */
public interface Spark {

    /**
     * Gets the CPU usage statistic for the current process.
     *
     * @return the CPU process statistic
     */
    @NonNull DoubleStatistic<CpuUsage> cpuProcess();

    /**
     * Gets the CPU usage statistic for the overall system.
     *
     * @return the CPU system statistic
     */
    @NonNull DoubleStatistic<CpuUsage> cpuSystem();

    /**
     * Gets the ticks per second statistic.
     *
     * <p>Returns {@code null} if the statistic is not supported.</p>
     *
     * @return the ticks per second statistic
     */
    @Nullable DoubleStatistic<TicksPerSecond> tps();

    /**
     * Gets the milliseconds per tick statistic.
     *
     * <p>Returns {@code null} if the statistic is not supported.</p>
     *
     * @return the milliseconds per tick statistic
     */
    @Nullable GenericStatistic<DoubleAverageInfo, MillisPerTick> mspt();

    /**
     * Gets the garbage collector statistics.
     *
     * @return the garbage collector statistics
     */
    @NonNull @Unmodifiable Map<String, GarbageCollector> gc();

}
