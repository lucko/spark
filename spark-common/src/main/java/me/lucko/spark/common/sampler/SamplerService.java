package me.lucko.spark.common.sampler;

import me.lucko.spark.api.profiler.Dumper;
import me.lucko.spark.api.profiler.Profiler;
import me.lucko.spark.api.profiler.ProfilerConfiguration;
import me.lucko.spark.api.profiler.ProfilerReport;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.dumper.PatternThreadDumper;
import me.lucko.spark.common.sampler.dumper.SpecificThreadDumper;
import me.lucko.spark.common.tick.TickHook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Set;

public class SamplerService implements Profiler {

    public static final int MINIMUM_DURATION = 10;
    public static final String THREAD_WILDCARD = "*";

    private final SparkPlatform platform;

    private Sampler active = null;

    public SamplerService(SparkPlatform platform) {
        this.platform = platform;
    }

    @Override
    public boolean start(ProfilerConfiguration configuration) {
        if (active != null) {
            return false;
        }

        Duration duration = configuration.duration();
        if (duration != null && duration.getSeconds() <= MINIMUM_DURATION) {
            return false;
        }

        double interval = configuration.interval();
        if (interval <= 0) {
            return false;
        }

        ThreadDumper dumper = getThreadDumper(configuration.dumper());
        ThreadGrouper grouper = ThreadGrouper.get(configuration.grouper());

        int minimum = configuration.minimumTickDuration();
        if (minimum >= 0) {
            TickHook hook = platform.getTickHook();
            if (hook == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public @Nullable ProfilerReport stop() {
        return null;
    }

    @NotNull
    private ThreadDumper getThreadDumper(@Nullable Dumper dumper) {
        if (dumper == null) {
            return platform.getPlugin().getDefaultThreadDumper();
        }

        Set<String> criteria = dumper.criteria();
        if (criteria.contains(THREAD_WILDCARD)) {
            return ThreadDumper.ALL;
        }

        Dumper.Method method = dumper.method();
        if (Dumper.Method.PATTERN.equals(method)) {
            return new PatternThreadDumper(criteria);
        }

        return new SpecificThreadDumper(criteria);
    }
}
