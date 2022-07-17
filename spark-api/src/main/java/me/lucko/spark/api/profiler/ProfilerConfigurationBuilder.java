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

package me.lucko.spark.api.profiler;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.profiler.dumper.ThreadDumper;
import me.lucko.spark.api.profiler.thread.ThreadGrouper;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * A builder for {@link ProfilerConfiguration profiler configurations}.
 *
 * @see Spark#configurationBuilder()
 */
@CanIgnoreReturnValue
@SuppressWarnings("UnusedReturnValue")
public interface ProfilerConfigurationBuilder {
    /**
     * Set the sampling interval to a given value or 4 if value is below 0. <br>
     * Note: the interval is in milliseconds
     *
     * @param samplingInterval the interval
     * @return the builder instance
     */
    ProfilerConfigurationBuilder samplingInterval(double samplingInterval);

    /**
     * Sets the duration of the profiler.
     *
     * @param duration the duration
     * @return the builder instance
     */
    ProfilerConfigurationBuilder duration(Duration duration);

    /**
     * Set the minimum tick duration that will be profiled.
     * If the minimumTickDuration is lower than 0 (default is -1), all ticks will be recorded.
     *
     * @param minimumTickDuration the minimum tick duration
     * @return the builder instance
     */
    ProfilerConfigurationBuilder minimumTickDuration(int minimumTickDuration);

    /**
     * Set the {@link ThreadGrouper grouper} used to sort the report.
     *
     * @param threadGrouper the grouper
     * @return the builder instance
     */
    ProfilerConfigurationBuilder grouper(@Nullable ThreadGrouper threadGrouper);

    /**
     * Set the {@link ThreadDumper dumper} used to generate the report.
     *
     * @param threadDumper the dumper
     * @return the builder instance
     */
    ProfilerConfigurationBuilder dumper(@Nullable ThreadDumper threadDumper);

    /**
     * Makes the configuration to ignore sleeping threads.
     *
     * @return the builder instance
     */
    ProfilerConfigurationBuilder ignoreSleeping();

    /**
     * Makes the configuration to ignore native threads.
     *
     * @return the builder instance
     */
    ProfilerConfigurationBuilder ignoreNative();

    /**
     * Forces the configuration to use a non-async java sampler.
     *
     * @return the builder instance
     */
    ProfilerConfigurationBuilder forceJavaSampler();

    /**
     * Builds the configuration.
     *
     * @return the built configuration
     */
    ProfilerConfiguration build();
}
