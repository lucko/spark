package me.lucko.spark.api.profiler;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public interface ProfilerConfiguration {

    static ProfilerConfigurationBuilder builder() {
        return new ProfilerConfigurationBuilder();
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
    DumperChoice dumper();

    /**
     * Get the choice of which thread grouper (AS_ONE, BY_NAME, BY_POOL) to use for this profiler.
     * If the grouper is null, BY_POOL is used.
     *
     * @return the thread grouper choice
     */
    @Nullable
    GrouperChoice grouper();
}
