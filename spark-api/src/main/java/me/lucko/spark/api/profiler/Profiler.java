package me.lucko.spark.api.profiler;

import org.jetbrains.annotations.Nullable;

public interface Profiler {

    /**
     * Start the Spark Profiler using a profiler configuration.
     * A configuration can be built using {@link ProfilerConfiguration#builder()}.
     *
     * @param configuration the configuration object
     * @return if the profiler started successfully
     */
    boolean start(ProfilerConfiguration configuration);

    /**
     * Stop the currently running profiler.
     *
     * @return the profiler report or null if no profiler was running.
     */
    @Nullable
    ProfilerReport stop();
}
