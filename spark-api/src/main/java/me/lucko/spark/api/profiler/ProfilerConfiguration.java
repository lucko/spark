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

import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.profiler.dumper.ThreadDumper;
import me.lucko.spark.api.profiler.thread.ThreadGrouper;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * Configuration for {@link Profiler profilers}.
 */
public interface ProfilerConfiguration {
    static ProfilerConfigurationBuilder builder() {
        return SparkProvider.get().configurationBuilder();
    }

    /**
     * Get the interval (in millis) of when the profiler should take samples.
     *
     * @return the sample interval
     */
    double interval();

    /**
     * Get if sleeping threads should be ignored.
     *
     * @return if sleeping threads are ignored
     */
    boolean ignoreSleeping();

    /**
     * Get if native threads should be ignored.
     *
     * @return if native threads are ignored
     */
    boolean ignoreNative();

    /**
     * Get if the native Java sampler should be used.
     *
     * @return if the native Java sampler is used
     */
    boolean forceJavaSampler();

    /**
     * Minimum duration (in millis) a tick has to take in order to be recorded.
     * If this value is below 0, all ticks will be recorded.
     *
     * @return the minimum tick duration
     */
    int minimumTickDuration();

    /**
     * Get how long the profiler should run, if the duration is null, the profiler runs indefinite.
     *
     * @return duration of the profile or null if indefinite
     */
    @Nullable
    Duration duration();

    /**
     * Get the choice of which dumper to use (i.e. ALL, Regex or Specific).
     * If no dumper is defined, ALL is used.
     *
     * @return the thread dumper choice
     */
    @Nullable
    ThreadDumper dumper();

    /**
     * Get the choice of which thread grouper ({@link ThreadGrouper#AS_ONE}, {@link ThreadGrouper#BY_NAME}, {@link ThreadGrouper#BY_POOL}) to use for this profiler.
     * If the grouper is null, BY_POOL is used.
     *
     * @return the thread grouper choice
     */
    @Nullable
    ThreadGrouper grouper();
}
