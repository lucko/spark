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
        MINUTES_1(Duration.ofMinutes(1)),
        MINUTES_5(Duration.ofMinutes(5));

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
