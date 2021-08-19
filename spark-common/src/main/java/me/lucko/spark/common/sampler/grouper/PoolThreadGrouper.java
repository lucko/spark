package me.lucko.spark.common.sampler.grouper;

import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.proto.SparkProtos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link ThreadGrouper} that attempts to group by the name of the pool
 * the thread originated from.
 *
 * <p>The regex pattern used to match pools expects a digit at the end of the thread name,
 * separated from the pool name with any of one or more of ' ', '-', or '#'.</p>
 */
public class PoolThreadGrouper implements ThreadGrouper {

    private final Map<Long, String> cache = new ConcurrentHashMap<>();
    private final Pattern pattern = Pattern.compile("^(.*?)[-# ]+\\d+$");

    @Override
    public String getGroup(long threadId, String threadName) {
        String group = this.cache.get(threadId);
        if (group != null) {
            return group;
        }

        Matcher matcher = this.pattern.matcher(threadName);
        if (!matcher.matches()) {
            return threadName;
        }

        group = matcher.group(1).trim() + " (Combined)";
        this.cache.put(threadId, group); // we don't care about race conditions here
        return group;
    }

    @Override
    public SparkProtos.SamplerMetadata.DataAggregator.ThreadGrouper asProto() {
        return SparkProtos.SamplerMetadata.DataAggregator.ThreadGrouper.BY_POOL;
    }
}
