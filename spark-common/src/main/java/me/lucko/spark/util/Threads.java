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

package me.lucko.spark.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Utilities for working with {@link Thread}s.
 */
public final class Threads {

    private Threads() {}

    private static final ThreadGroup ROOT_THREAD_GROUP;

    static {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ((parentGroup = rootGroup.getParent()) != null) {
            rootGroup = parentGroup;
        }
        ROOT_THREAD_GROUP = rootGroup;
    }

    /**
     * Gets a stream of all known active threads.
     *
     * @return a stream of threads
     */
    public static Stream<Thread> getThreads() {
        Thread[] threads = new Thread[ROOT_THREAD_GROUP.activeCount()];
        while (ROOT_THREAD_GROUP.enumerate(threads, true ) == threads.length) {
            threads = new Thread[threads.length * 2];
        }
        return Arrays.stream(threads).filter(Objects::nonNull);
    }

}
