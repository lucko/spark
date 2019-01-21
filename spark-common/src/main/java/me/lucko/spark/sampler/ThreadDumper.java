/*
 * This file is part of spark.
 *
 *  Copyright (C) Albert Pham <http://www.sk89q.com>
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

package me.lucko.spark.sampler;

import me.lucko.spark.util.ThreadFinder;

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
 * Uses the {@link ThreadMXBean} to generate {@link ThreadInfo} instances for the threads being
 * sampled.
 */
@FunctionalInterface
public interface ThreadDumper {

    /**
     * Generates {@link ThreadInfo} data for the sampled threads.
     *
     * @param threadBean the thread bean instance to obtain the data from
     * @return an array of generated thread info instances
     */
    ThreadInfo[] dumpThreads(ThreadMXBean threadBean);

    /**
     * Implementation of {@link ThreadDumper} that generates data for all threads.
     */
    ThreadDumper ALL = threadBean -> threadBean.dumpAllThreads(false, false);

    /**
     * Implementation of {@link ThreadDumper} that generates data for a specific set of threads.
     */
    final class Specific implements ThreadDumper {
        private final ThreadFinder threadFinder = new ThreadFinder();
        private final long[] ids;

        public Specific(long[] ids) {
            this.ids = ids;
        }

        public Specific(Set<String> names) {
            Set<String> namesLower = names.stream().map(String::toLowerCase).collect(Collectors.toSet());
            this.ids = this.threadFinder.getThreads()
                    .filter(t -> namesLower.contains(t.getName().toLowerCase()))
                    .mapToLong(Thread::getId)
                    .toArray();
        }

        @Override
        public ThreadInfo[] dumpThreads(ThreadMXBean threadBean) {
            return threadBean.getThreadInfo(this.ids, Integer.MAX_VALUE);
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
                    .map(regex -> {
                        try {
                            return Pattern.compile(regex);
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
                    .map(thread -> threadBean.getThreadInfo(thread.getId()))
                    .filter(Objects::nonNull)
                    .toArray(ThreadInfo[]::new);
        }
    }

}
