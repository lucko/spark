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

import me.lucko.spark.api.profiler.report.ProfilerReport;
import me.lucko.spark.api.profiler.report.ReportConfiguration;
import me.lucko.spark.api.util.ErrorHandler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The base interface of profilers. <br>
 * Profilers monitor the activity of the JVM, using {@link Sampler samplers}.
 *
 * @see me.lucko.spark.api.Spark#profiler(int)
 */
public interface Profiler {
    /**
     * Generates a new {@link Sampler}. <br>
     * Note: <strong>the sampler is not started by default</strong>, use {@link Sampler#start()}. <br>
     * This method is thread-safe.
     *
     * @param configuration the configuration to use for the profiler
     * @param errorReporter a consumer that reports any errors encountered in the creation of the sampler
     * @return the sampler, or if a validation error was caught, {@code null}
     */
    @Nullable
    Sampler createSampler(ProfilerConfiguration configuration, ErrorHandler errorReporter);

    /**
     * Gets the active samplers of this profiler.
     *
     * @return the active samplers
     */
    @Unmodifiable
    List<Sampler> activeSamplers();

    /**
     * Gets the maximum amount of samplers managed by this profiler.
     *
     * @return the maximum amount of samplers
     */
    int maxSamplers();

    /**
     * Stops this profiler and any {@link #activeSamplers() active children}. <br>
     * Note that {@link Sampler#onCompleted() completion callbacks} will not be completed.
     *
     * @see Sampler#stop()
     */
    void stop();

    /**
     * Represents a sampler used for profiling.
     */
    interface Sampler {
        /**
         * The minimum amount of seconds a sampler may run for.
         */
        int MINIMUM_DURATION = 10;

        /**
         * Starts the sampler.
         */
        void start();

        /**
         * Stops the sampler. <br>
         * Note that {@link #onCompleted() completion callbacks} will not be completed.
         */
        void stop();

        /**
         * Gets the time when the sampler started (unix timestamp in millis)
         *
         * @return the start time
         */
        long getStartTime();

        /**
         * Gets the time when the sampler should automatically stop (unix timestamp in millis)
         *
         * @return the end time, or -1 if undefined
         */
        long getAutoEndTime();

        /**
         * Gets a future that encapsulates the completion of the sampler, containing the report. <br>
         * Note: this future will not be completed unless this sampler is configured to automatically stop.
         *
         * @param configuration the configuration to use for generating the report
         * @return a future
         * @see #onCompleted()
         */
        CompletableFuture<ProfilerReport> onCompleted(ReportConfiguration configuration);

        /**
         * Gets a future that encapsulates the completion of the sampler, containing the sampler.
         * Note: this future will not be completed unless this sampler is configured to automatically stop.
         *
         * @return a future
         * @see #onCompleted(ReportConfiguration)
         */
        CompletableFuture<Sampler> onCompleted();

        /**
         * Dumps the report of the sampler. <br>
         * Note: make sure to {@link #stop() stop} the sampler before generating the report.
         *
         * @param configuration the configuration to use for generating the report
         * @return the report of the sampler
         */
        ProfilerReport dumpReport(ReportConfiguration configuration);

        /**
         * Checks if this sampler is an async sampler.
         *
         * @return if this sampler is an async sampler
         */
        boolean isAsync();
    }
}
