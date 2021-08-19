package me.lucko.spark.common.sampler.dumper;

import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.util.ThreadFinder;
import me.lucko.spark.proto.SparkProtos;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ThreadDumper} that generates data for a regex matched set of threads.
 */
public final class PatternThreadDumper implements ThreadDumper {

    private final ThreadFinder threadFinder = new ThreadFinder();
    private final Set<Pattern> namePatterns;
    private final Map<Long, Boolean> cache = new HashMap<>();

    public PatternThreadDumper(Set<String> namePatterns) {
        this.namePatterns = namePatterns.stream()
                .map(regex -> {
                    try {
                        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    } catch (PatternSyntaxException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public ThreadInfo[] dumpThreads(ThreadMXBean threadBean) {
        return this.threadFinder.getThreads()
                .filter(thread -> {
                    Boolean result = this.cache.get(thread.getId());
                    if (result != null) {
                        return result;
                    }

                    for (Pattern pattern : this.namePatterns) {
                        if (pattern.matcher(thread.getName()).matches()) {
                            this.cache.put(thread.getId(), true);
                            return true;
                        }
                    }
                    this.cache.put(thread.getId(), false);
                    return false;
                })
                .map(thread -> threadBean.getThreadInfo(thread.getId(), Integer.MAX_VALUE))
                .filter(Objects::nonNull)
                .toArray(ThreadInfo[]::new);
    }

    @Override
    public SparkProtos.SamplerMetadata.ThreadDumper getMetadata() {
        return SparkProtos.SamplerMetadata.ThreadDumper.newBuilder()
                .setType(SparkProtos.SamplerMetadata.ThreadDumper.Type.REGEX)
                .addAllPatterns(this.namePatterns.stream().map(Pattern::pattern).collect(Collectors.toList()))
                .build();
    }
}
