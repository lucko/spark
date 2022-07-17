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
