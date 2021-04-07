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

package me.lucko.spark.api;

import me.lucko.spark.api.gc.GarbageCollector;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import me.lucko.spark.api.statistic.StatisticWindow;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;

/**
 * The spark API.
 */
public interface Spark {

    /**
     * Gets the CPU usage statistic for the current process.
     *
     * @return the CPU process statistic
     */
    @NonNull DoubleStatistic<StatisticWindow.CpuUsage> cpuProcess();

    /**
     * Gets the CPU usage statistic for the overall system.
     *
     * @return the CPU system statistic
     */
    @NonNull DoubleStatistic<StatisticWindow.CpuUsage> cpuSystem();

    /**
     * Gets the ticks per second statistic.
     *
     * <p>Returns {@code null} if the statistic is not supported.</p>
     *
     * @return the ticks per second statistic
     */
    @Nullable DoubleStatistic<StatisticWindow.TicksPerSecond> tps();

    /**
     * Gets the milliseconds per tick statistic.
     *
     * <p>Returns {@code null} if the statistic is not supported.</p>
     *
     * @return the milliseconds per tick statistic
     */
    @Nullable GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt();

    /**
     * Gets the garbage collector statistics.
     *
     * @return the garbage collector statistics
     */
    @NonNull @Unmodifiable Map<String, GarbageCollector> gc();

}
