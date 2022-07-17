package me.lucko.spark.api.profiler;

import me.lucko.spark.api.profiler.report.ProfilerReport;
import me.lucko.spark.api.profiler.report.ReportConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A profilers used for sampling.
 */
public interface Profiler {
    /**
     * Generates a new {@link Sampler}. <br>
     * Note: <strong>the sampler is not started by default</strong>, use {@link Sampler#start()}
     *
     * @param configuration the configuration to use for the profiler
     * @param errorReporter a consumer that reports any errors encountered in the creation of the sampler
     * @return the sampler, or if a validation error was caught, {@code null}
     */
    @Nullable
    Sampler create(ProfilerConfiguration configuration, Consumer<String> errorReporter);

    /**
     * Represents a sampler used for profiling.
     */
    interface Sampler {
        /**
         * Gets a future to encapsulate the completion of the sampler, containing the report.
         *
         * @param configuration the configuration to use for generating the report
         * @return a future
         */
        CompletableFuture<ProfilerReport> whenDone(ReportConfiguration configuration);

        /**
         * Stops the sampler.
         */
        void stop();

        /**
         * Dumps the report of the sampler.
         *
         * @param configuration the configuration to use for generating the report
         * @return the report of the sampler
         */
        ProfilerReport dumpReport(ReportConfiguration configuration);

        /**
         * Starts the sampler.
         */
        void start();
    }
}
