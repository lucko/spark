package me.lucko.spark.common.sampler;

import me.lucko.spark.api.profiler.Profiler;
import me.lucko.spark.api.profiler.ProfilerConfiguration;
import me.lucko.spark.api.profiler.dumper.RegexThreadDumper;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.async.AsyncProfilerAccess;
import me.lucko.spark.common.sampler.async.AsyncSampler;
import me.lucko.spark.common.sampler.java.JavaSampler;
import me.lucko.spark.common.tick.TickHook;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public class ProfilerService implements Profiler {
    private final SparkPlatform platform;

    public static final int MINIMUM_DURATION = 10;

    private me.lucko.spark.common.sampler.Sampler active;

    public ProfilerService(SparkPlatform platform) {
        this.platform = platform;
    }

    @Override
    public me.lucko.spark.common.sampler.Sampler create(ProfilerConfiguration configuration, Consumer<String> err) {
        if (active != null) {
            err.accept("A profiler is already running!");
            return null;
        }

        Duration duration = configuration.duration();
        if (duration == null)
            duration = Duration.of(MINIMUM_DURATION, ChronoUnit.SECONDS);
        if (duration.getSeconds() <= MINIMUM_DURATION) {
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
        if (minimum >= 1) {
            sampler = new JavaSampler(platform, intervalMicros, configuration.dumper(), configuration.grouper(), timeout, configuration.ignoreSleeping(), configuration.ignoreNative(), hook, configuration.minimumTickDuration());
        } else if (!configuration.forceJavaSampler() && !(configuration.dumper() instanceof RegexThreadDumper) && AsyncProfilerAccess.INSTANCE.checkSupported(platform)) {
            sampler = new AsyncSampler(platform, intervalMicros, configuration.dumper(), configuration.grouper(), timeout);
        } else {
            sampler = new JavaSampler(platform, intervalMicros, configuration.dumper(), configuration.grouper(), timeout, configuration.ignoreSleeping(), configuration.ignoreNative());
        }
        // set activeSampler to null when complete.
        sampler.getFuture().whenCompleteAsync((s, throwable) -> {
            if (sampler == this.active) {
                this.active = null;
            }
        });

        return active = sampler;
    }

    public me.lucko.spark.common.sampler.Sampler active() {
        return active;
    }
    public void clear() {
        if (active != null) {
            active = null;
        }
    }
    public void clearAndStop() {
        if (active != null) {
            active.stop();
            active = null;
        }
    }

    private static long computeTimeout(Duration duration) {
        return System.currentTimeMillis() + duration.toMillis();
    }
}
