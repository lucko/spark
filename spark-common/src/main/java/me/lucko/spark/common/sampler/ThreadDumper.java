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

package me.lucko.spark.common.sampler;

import com.google.gson.stream.JsonWriter;
import me.lucko.spark.common.util.ThreadFinder;

import java.io.IOException;
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
public interface ThreadDumper {

    /**
     * Generates {@link ThreadInfo} data for the sampled threads.
     *
     * @param threadBean the thread bean instance to obtain the data from
     * @return an array of generated thread info instances
     */
    ThreadInfo[] dumpThreads(ThreadMXBean threadBean);

    /**
     * Writes metadata about the thread dumper instance to the given {@code writer}.
     *
     * @param writer the writer
     * @throws IOException if thrown by the writer
     */
    void writeMetadata(JsonWriter writer) throws IOException;

    /**
     * Implementation of {@link ThreadDumper} that generates data for all threads.
     */
    ThreadDumper ALL = new ThreadDumper() {
        @Override
        public ThreadInfo[] dumpThreads(final ThreadMXBean threadBean) {
            return threadBean.dumpAllThreads(false, false);
        }

        @Override
        public void writeMetadata(JsonWriter writer) throws IOException {
            writer.name("type").value("all");
        }
    };

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

        @Override
        public void writeMetadata(JsonWriter writer) throws IOException {
            writer.name("type").value("specific");
            writer.name("ids").beginArray();
            for (long id : ids) {
                writer.value(id);
            }
            writer.endArray();
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

        @Override
        public void writeMetadata(JsonWriter writer) throws IOException {
            writer.name("type").value("regex");
            writer.name("patterns").beginArray();
            for (Pattern pattern : namePatterns) {
                writer.value(pattern.pattern());
            }
            writer.endArray();
        }
    }

}
