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
import me.lucko.spark.api.util.ErrorHandler;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.async.AsyncProfilerAccess;
import me.lucko.spark.common.sampler.async.AsyncSampler;
import me.lucko.spark.common.sampler.java.JavaSampler;
import me.lucko.spark.common.tick.TickHook;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProfilerService implements Profiler, SamplerManager {
    private final SparkPlatform platform;

    public static final int MINIMUM_DURATION = 10;

    private final int maxSamplers;
    private final List<Sampler> active;
    private final List<Sampler> activeView;

    public ProfilerService(SparkPlatform platform, int samplerAmount) {
        if (samplerAmount <= 0)
            throw new IllegalArgumentException("samplerAmount <= 0");

        this.platform = platform;
        this.maxSamplers = samplerAmount;
        this.active = new CopyOnWriteArrayList<>();
        this.activeView = Collections.unmodifiableList(active);
    }

    @Override
    public Sampler createSampler(ProfilerConfiguration configuration, ErrorHandler err) {
        if (active.size() >= maxSamplers) {
            if (maxSamplers == 1) {
                err.accept("A profiling sampler is already running!");
            } else {
                err.accept(String.format("Maximum amount of %s profiling samplers are already running!", active.size()));
            }
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

        final me.lucko.spark.common.sampler.Sampler sampler;
        if (minimum >= 1) {
            sampler = new JavaSampler(this, platform, intervalMicros, configuration.dumper(), configuration.grouper(), timeout, configuration.ignoreSleeping(), configuration.ignoreNative(), hook, configuration.minimumTickDuration());
        } else if (!configuration.forceJavaSampler() && !(configuration.dumper() instanceof RegexThreadDumper) && AsyncProfilerAccess.INSTANCE.checkSupported(platform)) {
            sampler = new AsyncSampler(this, platform, intervalMicros, configuration.dumper(), configuration.grouper(), timeout);
        } else {
            sampler = new JavaSampler(this, platform, intervalMicros, configuration.dumper(), configuration.grouper(), timeout, configuration.ignoreSleeping(), configuration.ignoreNative());
        }

        return sampler;
    }

    @Override
    public List<Sampler> activeSamplers() {
        return activeView;
    }

    @Override
    public int maxSamplers() {
        return maxSamplers;
    }

    @Override
    public void stop() {
        // Prevent concurrent modifications
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < active.size(); i++) {
            active.get(i).stop();
        }
    }

    private static long computeTimeout(@Nullable Duration duration) {
        if (duration == null)
            return -1;
        return System.currentTimeMillis() + duration.toMillis();
    }

    @Override
    public void markStopped(Sampler sampler) {
        active.remove(sampler);
    }

    @Override
    public void markStarted(Sampler sampler) {
        if (active.size() >= maxSamplers)
            throw new ArrayIndexOutOfBoundsException("Maximum amount of active samplers has been reached!");
        active.add(sampler);
    }
}
