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

import me.lucko.spark.api.profiler.Profiler;
import me.lucko.spark.api.profiler.ProfilerConfiguration;
import me.lucko.spark.api.profiler.dumper.RegexThreadDumper;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.async.AsyncProfilerAccess;
import me.lucko.spark.common.sampler.async.AsyncSampler;
import me.lucko.spark.common.sampler.java.JavaSampler;
import me.lucko.spark.common.tick.TickHook;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.function.Consumer;

public class ProfilerService implements Profiler {
    private final SparkPlatform platform;

    public static final int MINIMUM_DURATION = 10;

    private me.lucko.spark.common.sampler.Sampler active;

    public ProfilerService(SparkPlatform platform) {
        this.platform = platform;
    }

    @Override
    public me.lucko.spark.common.sampler.Sampler createSampler(ProfilerConfiguration configuration, Consumer<String> err) {
        if (active != null) {
            err.accept("A profiler is already running!");
            return null;
        }

        Duration duration = configuration.duration();
        if (duration != null && duration.getSeconds() < MINIMUM_DURATION) {
            err.accept("A profiler needs to run for at least " + MINIMUM_DURATION + " seconds!");
            return null;
        }

        double interval = configuration.interval();
        if (interval <= 0) {
            err.accept("Cannot run profiler with negative interval.");
            return null;
        }

        TickHook hook = null;
        int minimum = configuration.minimumTickDuration();
        if (minimum >= 0) {
            hook = platform.getTickHook();
            if (hook == null) {
                err.accept("Tick counting is not supported!");
                return null;
            }
        }

        final int intervalMicros = (int) (interval * 1000d);
        final long timeout = computeTimeout(duration);
        me.lucko.spark.common.sampler.Sampler sampler;
        // set activeSampler to null when stopped.
        final Consumer<me.lucko.spark.common.sampler.Sampler> whenStopped = s -> {
            if (s == this.active)
                this.active = null;
        };
        if (minimum >= 1) {
            sampler = new JavaSampler(whenStopped, platform, intervalMicros, configuration.dumper(), configuration.grouper(), timeout, configuration.ignoreSleeping(), configuration.ignoreNative(), hook, configuration.minimumTickDuration());
        } else if (!configuration.forceJavaSampler() && !(configuration.dumper() instanceof RegexThreadDumper) && AsyncProfilerAccess.INSTANCE.checkSupported(platform)) {
            sampler = new AsyncSampler(whenStopped, platform, intervalMicros, configuration.dumper(), configuration.grouper(), timeout);
        } else {
            sampler = new JavaSampler(whenStopped, platform, intervalMicros, configuration.dumper(), configuration.grouper(), timeout, configuration.ignoreSleeping(), configuration.ignoreNative());
        }

        return active = sampler;
    }

    @Override
    public me.lucko.spark.common.sampler.Sampler activeSampler() {
        return active;
    }

    private static long computeTimeout(@Nullable Duration duration) {
        if (duration == null)
            return -1;
        return System.currentTimeMillis() + duration.toMillis();
    }
}
