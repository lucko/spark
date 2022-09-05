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
import me.lucko.spark.api.heap.HeapAnalysis;
import me.lucko.spark.api.ping.PingStatistics;
import me.lucko.spark.api.profiler.Profiler;
import me.lucko.spark.api.profiler.ProfilerConfigurationBuilder;
import me.lucko.spark.api.profiler.thread.ThreadGrouper;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import me.lucko.spark.api.util.StreamSupplier;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata.DataAggregator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;

import static me.lucko.spark.api.statistic.StatisticWindow.CpuUsage;
import static me.lucko.spark.api.statistic.StatisticWindow.MillisPerTick;
import static me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond;

/**
 * The spark API.
 * @see #get()
 */
public interface Spark {

    /**
     * Gets the singleton spark API instance.
     *
     * @return the spark API instance
     * @see SparkProvider#get()
     */
    static @NonNull Spark get() {
        return SparkProvider.get();
    }

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

    /**
     * Creates a thread finder.
     *
     * @return a thread finder
     */
    @NonNull StreamSupplier<Thread> threadFinder();

    /**
     * Creates a new {@link ProfilerConfigurationBuilder profiler configuration builder}.
     *
     * @return the builder
     */
    @NonNull ProfilerConfigurationBuilder configurationBuilder();

    /**
     * Creates a new {@link Profiler profiler}.
     *
     * @param maxSamplers the maximum amount of active samplers the profiler can manage
     * @return the profiler
     * @throws IllegalArgumentException if {@code maxSamplers <= 0}
     */
    @NonNull Profiler profiler(int maxSamplers);

    /**
     * Gets a {@link HeapAnalysis} instance.
     *
     * @return the heap analysis instance
     */
    @NonNull HeapAnalysis heapAnalysis();

    /**
     * Gets a {@link PingStatistics} instance.
     *
     * @return the ping statistics instance, or {@code null} if the platform cannot provide that info
     */
    @Nullable PingStatistics ping();

    /**
     * Gets the {@link ThreadGrouper} associated with a Proto {@link DataAggregator.ThreadGrouper}.
     *
     * @param type the Proto type
     * @return the grouper
     * @see ThreadGrouper#BY_POOL
     * @see ThreadGrouper#BY_NAME
     * @see ThreadGrouper#AS_ONE
     * @throws AssertionError if the type is {@link DataAggregator.ThreadGrouper#UNRECOGNIZED unknown}.
     */
    @ApiStatus.Internal
    @NonNull ThreadGrouper grouper(DataAggregator.ThreadGrouper type);
}