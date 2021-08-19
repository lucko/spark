package me.lucko.spark.api.profiler;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public class ProfilerConfigurationBuilder {

    private double interval = 5;
    private boolean ignoreSleeping = false;
    private boolean ignoreNative = false;
    private boolean forceJavaSampler = true;
    private int minimumTickDuration = -1;
    private @Nullable Duration duration = null;
    private @Nullable Dumper dumper = null;
    private @Nullable GrouperChoice grouper = null;

    /**
     * Set the interval to a given value or 5 if value is below 0.
     *
     * @param interval the interval
     * @return the builder instance
     */
    public ProfilerConfigurationBuilder interval(double interval) {
        this.interval = interval > 0 ? interval : 5;
        return this;
    }

    public ProfilerConfigurationBuilder ignoreSleeping(boolean ignoreSleeping) {
        this.ignoreSleeping = ignoreSleeping;
        return this;
    }

    public ProfilerConfigurationBuilder ignoreNative(boolean ignoreNative) {
        this.ignoreNative = ignoreNative;
        return this;
    }

    public ProfilerConfigurationBuilder forceJavaSampler(boolean forceJavaSampler) {
        this.forceJavaSampler = forceJavaSampler;
        return this;
    }

    /**
     * Set the minimum tick duration that will be profiled.
     * If the minimumTickDuration is lower than 0 (default is -1), all ticks will be recorded.
     *
     * @param minimumTickDuration the minimum tick duration
     * @return the builder instance
     */
    public ProfilerConfigurationBuilder minimumTickDuration(int minimumTickDuration) {
        this.minimumTickDuration = minimumTickDuration;
        return this;
    }

    public ProfilerConfigurationBuilder duration(Duration duration) {
        this.duration = duration;
        return this;
    }

    public ProfilerConfigurationBuilder dumper(Dumper dumper) {
        this.dumper = dumper;
        return this;
    }

    public ProfilerConfigurationBuilder grouper(GrouperChoice grouper) {
        this.grouper = grouper;
        return this;
    }

    public ProfilerConfiguration build() {
        return new ProfilerConfiguration() {
            @Override
            public double interval() {
                return interval;
            }

            @Override
            public boolean ignoreSleeping() {
                return ignoreSleeping;
            }

            @Override
            public boolean ignoreNative() {
                return ignoreNative;
            }

            @Override
            public boolean forceJavaSampler() {
                return forceJavaSampler;
            }

            @Override
            public int minimumTickDuration() {
                return minimumTickDuration;
            }

            @Override
            public @Nullable Duration duration() {
                return duration;
            }

            @Override
            public @Nullable Dumper dumper() {
                return dumper;
            }

            @Override
            public @Nullable GrouperChoice grouper() {
                return grouper;
            }
        };
    }
}
