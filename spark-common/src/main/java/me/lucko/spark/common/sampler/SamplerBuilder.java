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

package me.lucko.spark.common.sampler;

import me.lucko.spark.api.profiler.ProfilerConfiguration;
import me.lucko.spark.api.profiler.ProfilerConfigurationBuilder;
import me.lucko.spark.api.profiler.dumper.ThreadDumper;
import me.lucko.spark.api.profiler.thread.ThreadGrouper;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Builds {@link Sampler} instances.
 */
@SuppressWarnings("UnusedReturnValue")
public class SamplerBuilder implements ProfilerConfigurationBuilder {

    private double samplingInterval = 4; // milliseconds
    private boolean ignoreSleeping = false;
    private boolean ignoreNative = false;
    private boolean useAsyncProfiler = true;
    private Duration duration;
    private ThreadDumper threadDumper = ThreadDumper.ALL;
    private ThreadGrouper threadGrouper = ThreadGrouper.BY_NAME;

    private int minimumTickDuration = -1;

    public SamplerBuilder() {
    }

    public SamplerBuilder samplingInterval(double samplingInterval) {
        this.samplingInterval = samplingInterval <= 0 ? 4 : samplingInterval;
        return this;
    }

    public SamplerBuilder completeAfter(long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout > 0");
        }
        this.duration = Duration.of(timeout, toChronoUnit(unit));
        return this;
    }

    private static ChronoUnit toChronoUnit(TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:  return ChronoUnit.NANOS;
            case MICROSECONDS: return ChronoUnit.MICROS;
            case MILLISECONDS: return ChronoUnit.MILLIS;
            case SECONDS:      return ChronoUnit.SECONDS;
            case MINUTES:      return ChronoUnit.MINUTES;
            case HOURS:        return ChronoUnit.HOURS;
            case DAYS:         return ChronoUnit.DAYS;
            default: throw new AssertionError();
        }
    }

    @Override
    public SamplerBuilder duration(Duration duration) {
        return completeAfter(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public SamplerBuilder dumper(ThreadDumper threadDumper) {
        this.threadDumper = threadDumper;
        return this;
    }

    public SamplerBuilder grouper(ThreadGrouper threadGrouper) {
        this.threadGrouper = threadGrouper;
        return this;
    }

    @Override
    public SamplerBuilder minimumTickDuration(int duration) {
        this.minimumTickDuration = duration;
        return this;
    }

    public SamplerBuilder ignoreSleeping(boolean ignoreSleeping) {
        this.ignoreSleeping = ignoreSleeping;
        return this;
    }

    @Override
    public SamplerBuilder ignoreSleeping() {
        return ignoreSleeping(true);
    }

    public SamplerBuilder ignoreNative(boolean ignoreNative) {
        this.ignoreNative = ignoreNative;
        return this;
    }
    @Override
    public SamplerBuilder ignoreNative() {
        return ignoreNative(true);
    }

    public SamplerBuilder forceJavaSampler(boolean forceJavaSampler) {
        this.useAsyncProfiler = !forceJavaSampler;
        return this;
    }
    @Override
    public SamplerBuilder forceJavaSampler() {
        return forceJavaSampler(true);
    }

    @Override
    public ProfilerConfiguration build() {
        return new ProfilerConfiguration() {
            @Override
            public double interval() {
                return samplingInterval;
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
                return !useAsyncProfiler;
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
            public @Nullable ThreadDumper dumper() {
                return threadDumper;
            }

            @Override
            public @Nullable ThreadGrouper grouper() {
                return threadGrouper;
            }
        };
    }

}
