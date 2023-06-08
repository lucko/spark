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

package me.lucko.spark.common.api;

import com.google.common.collect.ImmutableMap;

import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.gc.GarbageCollector;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.monitor.cpu.CpuMonitor;
import me.lucko.spark.common.monitor.memory.GarbageCollectorStatistics;
import me.lucko.spark.common.monitor.tick.TickStatistics;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static me.lucko.spark.api.statistic.StatisticWindow.CpuUsage;
import static me.lucko.spark.api.statistic.StatisticWindow.MillisPerTick;
import static me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond;

public class SparkApi implements Spark {
    private static final Method SINGLETON_SET_METHOD;

    static {
        try {
            SINGLETON_SET_METHOD = SparkProvider.class.getDeclaredMethod("set", Spark.class);
            SINGLETON_SET_METHOD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private final SparkPlatform platform;

    public SparkApi(SparkPlatform platform) {
        this.platform = platform;
    }

    @Override
    public @NonNull DoubleStatistic<CpuUsage> cpuProcess() {
        return new AbstractStatistic.Double<CpuUsage>(
                "CPU Process Usage", CpuUsage.class
        ) {
            @Override
            public double poll(@NonNull CpuUsage window) {
                switch (window) {
                    case SECONDS_10:
                        return CpuMonitor.processLoad10SecAvg();
                    case MINUTES_1:
                        return CpuMonitor.processLoad1MinAvg();
                    case MINUTES_15:
                        return CpuMonitor.processLoad15MinAvg();
                    default:
                        throw new AssertionError(window);
                }
            }
        };
    }

    @Override
    public @NonNull DoubleStatistic<CpuUsage> cpuSystem() {
        return new AbstractStatistic.Double<CpuUsage>(
                "CPU System Usage", CpuUsage.class
        ) {
            @Override
            public double poll(@NonNull CpuUsage window) {
                switch (window) {
                    case SECONDS_10:
                        return CpuMonitor.systemLoad10SecAvg();
                    case MINUTES_1:
                        return CpuMonitor.systemLoad1MinAvg();
                    case MINUTES_15:
                        return CpuMonitor.systemLoad15MinAvg();
                    default:
                        throw new AssertionError(window);
                }
            }
        };
    }

    @Override
    public @Nullable DoubleStatistic<TicksPerSecond> tps() {
        TickStatistics stats = this.platform.getTickStatistics();
        if (stats == null) {
            return null;
        }

        return new AbstractStatistic.Double<TicksPerSecond>(
                "Ticks Per Second", TicksPerSecond.class
        ) {
            @Override
            public double poll(@NonNull TicksPerSecond window) {
                switch (window) {
                    case SECONDS_5:
                        return stats.tps5Sec();
                    case SECONDS_10:
                        return stats.tps10Sec();
                    case MINUTES_1:
                        return stats.tps1Min();
                    case MINUTES_5:
                        return stats.tps5Min();
                    case MINUTES_15:
                        return stats.tps15Min();
                    default:
                        throw new AssertionError(window);
                }
            }
        };
    }

    @Override
    public @Nullable GenericStatistic<DoubleAverageInfo, MillisPerTick> mspt() {
        TickStatistics stats = this.platform.getTickStatistics();
        if (stats == null || !stats.isDurationSupported()) {
            return null;
        }

        return new AbstractStatistic.Generic<DoubleAverageInfo, MillisPerTick>(
                "Milliseconds Per Tick", DoubleAverageInfo.class, MillisPerTick.class
        ) {
            @Override
            public DoubleAverageInfo poll(@NonNull MillisPerTick window) {
                switch (window) {
                    case SECONDS_10:
                        return stats.duration10Sec();
                    case MINUTES_1:
                        return stats.duration1Min();
                    case MINUTES_5:
                        return stats.duration5Min();
                    default:
                        throw new AssertionError(window);
                }
            }
        };
    }

    @Override
    public @NonNull Map<String, GarbageCollector> gc() {
        long serverUptime = System.currentTimeMillis() - this.platform.getServerNormalOperationStartTime();
        Map<String, GarbageCollectorStatistics> stats = GarbageCollectorStatistics.pollStatsSubtractInitial(
                this.platform.getStartupGcStatistics()
        );

        Map<String, GarbageCollector> map = new HashMap<>(stats.size());
        for (Map.Entry<String, GarbageCollectorStatistics> entry : stats.entrySet()) {
            map.put(entry.getKey(), new GarbageCollectorInfo(entry.getKey(), entry.getValue(), serverUptime));
        }
        return ImmutableMap.copyOf(map);
    }

    public static void register(Spark spark) {
        try {
            SINGLETON_SET_METHOD.invoke(null, spark);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static void unregister() {
        try {
            SINGLETON_SET_METHOD.invoke(null, new Object[]{null});
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}
