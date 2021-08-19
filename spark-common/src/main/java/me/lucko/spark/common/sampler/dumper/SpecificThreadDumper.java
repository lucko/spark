package me.lucko.spark.common.sampler.dumper;

import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.util.ThreadFinder;
import me.lucko.spark.proto.SparkProtos;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ThreadDumper} that generates data for a specific set of threads.
 */
public final class SpecificThreadDumper implements ThreadDumper {
    private final long[] ids;
    private Set<Thread> threads;
    private Set<String> threadNamesLowerCase;

    public SpecificThreadDumper(long[] ids) {
        this.ids = ids;
    }

    public SpecificThreadDumper(Set<String> names) {
        this.threadNamesLowerCase = names.stream().map(String::toLowerCase).collect(Collectors.toSet());
        this.ids = new ThreadFinder().getThreads()
                .filter(t -> this.threadNamesLowerCase.contains(t.getName().toLowerCase()))
                .mapToLong(Thread::getId)
                .toArray();
        Arrays.sort(this.ids);
    }

    public Set<Thread> getThreads() {
        if (this.threads == null) {
            this.threads = new ThreadFinder().getThreads()
                    .filter(t -> Arrays.binarySearch(this.ids, t.getId()) >= 0)
                    .collect(Collectors.toSet());
        }
        return this.threads;
    }

    public Set<String> getThreadNames() {
        if (this.threadNamesLowerCase == null) {
            this.threadNamesLowerCase = getThreads().stream()
                    .map(t -> t.getName().toLowerCase())
                    .collect(Collectors.toSet());
        }
        return this.threadNamesLowerCase;
    }

    @Override
    public ThreadInfo[] dumpThreads(ThreadMXBean threadBean) {
        return threadBean.getThreadInfo(this.ids, Integer.MAX_VALUE);
    }

    @Override
    public SparkProtos.SamplerMetadata.ThreadDumper getMetadata() {
        return SparkProtos.SamplerMetadata.ThreadDumper.newBuilder()
                .setType(SparkProtos.SamplerMetadata.ThreadDumper.Type.SPECIFIC)
                .addAllIds(Arrays.stream(this.ids).boxed().collect(Collectors.toList()))
                .build();
    }
}
