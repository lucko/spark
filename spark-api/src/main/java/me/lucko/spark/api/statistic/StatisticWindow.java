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

package me.lucko.spark.api.statistic;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * A window in which a statistic is recorded as a rolling average.
 */
public interface StatisticWindow {

    /**
     * Gets the length of the window as a {@link Duration}.
     *
     * @return the length of the window
     */
    @NonNull Duration length();

    /**
     * The {@link StatisticWindow} used for CPU usage.
     */
    enum CpuUsage implements StatisticWindow {

        SECONDS_10(Duration.ofSeconds(10)),
        MINUTES_1(Duration.ofMinutes(1)),
        MINUTES_15(Duration.ofMinutes(15));

        private final Duration value;

        CpuUsage(Duration value) {
            this.value = value;
        }

        @Override
        public @NotNull Duration length() {
            return this.value;
        }
    }

    /**
     * The {@link StatisticWindow} used for TPS.
     */
    enum TicksPerSecond implements StatisticWindow {

        SECONDS_5(Duration.ofSeconds(5)),
        SECONDS_10(Duration.ofSeconds(10)),
        MINUTES_1(Duration.ofMinutes(1)),
        MINUTES_5(Duration.ofMinutes(5)),
        MINUTES_15(Duration.ofMinutes(15));

        private final Duration value;

        TicksPerSecond(Duration value) {
            this.value = value;
        }

        @Override
        public @NotNull Duration length() {
            return this.value;
        }
    }

    /**
     * The {@link StatisticWindow} used for MSPT.
     */
    enum MillisPerTick implements StatisticWindow {

        SECONDS_10(Duration.ofSeconds(10)),
        MINUTES_1(Duration.ofMinutes(1));

        private final Duration value;

        MillisPerTick(Duration value) {
            this.value = value;
        }

        @Override
        public @NotNull Duration length() {
            return this.value;
        }
    }

}
