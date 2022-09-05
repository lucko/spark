/*
 * This file is part of spark, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.spark.api.profiler.dumper;

import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.proto.SparkSamplerProtos;

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

    public SpecificThreadDumper(Thread thread) {
        this.ids = new long[] {thread.getId()};
    }

    public SpecificThreadDumper(long[] ids) {
        this.ids = ids;
    }

    public SpecificThreadDumper(Set<String> names) {
        this.threadNamesLowerCase = names.stream().map(String::toLowerCase).collect(Collectors.toSet());
        this.ids = SparkProvider.get().threadFinder().get()
                .filter(t -> this.threadNamesLowerCase.contains(t.getName().toLowerCase()))
                .mapToLong(Thread::getId)
                .toArray();
        Arrays.sort(this.ids);
    }

    public Set<Thread> getThreads() {
        if (this.threads == null) {
            this.threads = SparkProvider.get().threadFinder().get()
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
    public SparkSamplerProtos.SamplerMetadata.ThreadDumper getMetadata() {
        return SparkSamplerProtos.SamplerMetadata.ThreadDumper.newBuilder()
                .setType(SparkSamplerProtos.SamplerMetadata.ThreadDumper.Type.SPECIFIC)
                .addAllIds(Arrays.stream(this.ids).boxed().collect(Collectors.toList()))
                .build();
    }
}
