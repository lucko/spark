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

import me.lucko.spark.common.util.ThreadFinder;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Uses the {@link ThreadMXBean} to generate {@link ThreadInfo} instances for the threads being
 * sampled.
 */
public interface ThreadDumper {

    /**
     * Generates {@link ThreadInfo} data for the sampled threads.
     *
     * @param threadBean the thread bean instance to obtain the data from
     * @return an array of generated thread info instances
     */
    ThreadInfo[] dumpThreads(ThreadMXBean threadBean);

    /**
     * Gets if the given thread should be included in the output.
     *
     * @param threadId the thread id
     * @param threadName the thread name
     * @return if the thread should be included
     */
    boolean isThreadIncluded(long threadId, String threadName);

    /**
     * Gets metadata about the thread dumper instance.
     */
    SamplerMetadata.ThreadDumper getMetadata();

    /**
     * Creates a new {@link ThreadDumper} by parsing the given config setting.
     *
     * @param setting the config setting
     * @return the thread dumper
     */
    static ThreadDumper parseConfigSetting(String setting) {
        switch (setting) {
            case "default":
                return null;
            case "all":
                return ALL;
            default:
                Set<String> threadNames = Arrays.stream(setting.split(",")).collect(Collectors.toSet());
                return new ThreadDumper.Specific(threadNames);
        }
    }

    /**
     * Implementation of {@link ThreadDumper} that generates data for all threads.
     */
    ThreadDumper ALL = new ThreadDumper() {
        @Override
        public ThreadInfo[] dumpThreads(final ThreadMXBean threadBean) {
            return threadBean.dumpAllThreads(false, false);
        }

        @Override
        public boolean isThreadIncluded(long threadId, String threadName) {
            return true;
        }

        @Override
        public SamplerMetadata.ThreadDumper getMetadata() {
            return SamplerMetadata.ThreadDumper.newBuilder()
                    .setType(SamplerMetadata.ThreadDumper.Type.ALL)
                    .build();
        }
    };

    /**
     * Utility to cache the creation of a {@link ThreadDumper} targeting
     * the game (server/client) thread.
     */
    final class GameThread implements Supplier<ThreadDumper> {
        private Supplier<Thread> threadSupplier;
        private Specific dumper = null;

        public GameThread() {

        }

        public GameThread(Supplier<Thread> threadSupplier) {
            this.threadSupplier = threadSupplier;
        }

        @Override
        public ThreadDumper get() {
            if (this.dumper == null) {
                setThread(this.threadSupplier.get());
                this.threadSupplier = null;
            }

            return Objects.requireNonNull(this.dumper, "dumper");
        }

        public void setThread(Thread thread) {
            this.dumper = new Specific(thread);
        }
    }

    /**
     * Implementation of {@link ThreadDumper} that generates data for a specific set of threads.
     */
    final class Specific implements ThreadDumper {
        private final long[] ids;
        private Set<Thread> threads;
        private Set<String> threadNamesLowerCase;

        public Specific(Thread thread) {
            this.ids = new long[]{thread.getId()};
        }

        public Specific(Set<String> names) {
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
        public boolean isThreadIncluded(long threadId, String threadName) {
            if (Arrays.binarySearch(this.ids, threadId) >= 0) {
                return true;
            }
            return getThreadNames().contains(threadName.toLowerCase());
        }

        @Override
        public ThreadInfo[] dumpThreads(ThreadMXBean threadBean) {
            return threadBean.getThreadInfo(this.ids, Integer.MAX_VALUE);
        }

        @Override
        public SamplerMetadata.ThreadDumper getMetadata() {
            return SamplerMetadata.ThreadDumper.newBuilder()
                    .setType(SamplerMetadata.ThreadDumper.Type.SPECIFIC)
                    .addAllIds(Arrays.stream(this.ids).boxed().collect(Collectors.toList()))
                    .build();
        }
    }

    /**
     * Implementation of {@link ThreadDumper} that generates data for a regex matched set of threads.
     */
    final class Regex implements ThreadDumper {
        private final ThreadFinder threadFinder = new ThreadFinder();
        private final Set<Pattern> namePatterns;
        private final Map<Long, Boolean> cache = new HashMap<>();

        public Regex(Set<String> namePatterns) {
            this.namePatterns = namePatterns.stream()
                    .map(regex -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
                    .collect(Collectors.toSet());
        }

        @Override
        public boolean isThreadIncluded(long threadId, String threadName) {
            Boolean result = this.cache.get(threadId);
            if (result != null) {
                return result;
            }

            for (Pattern pattern : this.namePatterns) {
                if (pattern.matcher(threadName).matches()) {
                    this.cache.put(threadId, true);
                    return true;
                }
            }
            this.cache.put(threadId, false);
            return false;
        }

        @Override
        public ThreadInfo[] dumpThreads(ThreadMXBean threadBean) {
            return this.threadFinder.getThreads()
                    .filter(thread -> isThreadIncluded(thread.getId(), thread.getName()))
                    .map(thread -> threadBean.getThreadInfo(thread.getId(), Integer.MAX_VALUE))
                    .filter(Objects::nonNull)
                    .toArray(ThreadInfo[]::new);
        }

        @Override
        public SamplerMetadata.ThreadDumper getMetadata() {
            return SamplerMetadata.ThreadDumper.newBuilder()
                    .setType(SamplerMetadata.ThreadDumper.Type.REGEX)
                    .addAllPatterns(this.namePatterns.stream().map(Pattern::pattern).collect(Collectors.toList()))
                    .build();
        }
    }

}
