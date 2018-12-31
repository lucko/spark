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
 * Utility to find active threads.
 */
public final class ThreadFinder {

    private static final ThreadGroup ROOT_THREAD_GROUP;
    static {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ((parentGroup = rootGroup.getParent()) != null) {
            rootGroup = parentGroup;
        }
        ROOT_THREAD_GROUP = rootGroup;
    }

    // cache the approx active count at the time of construction.
    // the usages of this class are likely to be somewhat short-lived, so it's good
    // enough to just cache a value on init.
    private final int approxActiveCount = ROOT_THREAD_GROUP.activeCount();

    /**
     * Gets a stream of all known active threads.
     *
     * @return a stream of threads
     */
    public Stream<Thread> getThreads() {
        Thread[] threads = new Thread[this.approxActiveCount + 20]; // +20 to allow a bit of growth for newly created threads
        while (ROOT_THREAD_GROUP.enumerate(threads, true ) == threads.length) {
            threads = new Thread[threads.length * 2];
        }
        return Arrays.stream(threads).filter(Objects::nonNull);
    }

}
