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

package me.lucko.spark.common.sampler.aggregator;

import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.node.ThreadNode;

import java.lang.management.ThreadInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public abstract class AbstractDataAggregator implements DataAggregator {

    /** A map of root stack nodes for each thread with sampling data */
    protected final Map<String, ThreadNode> threadData = new ConcurrentHashMap<>();

    /** The worker pool for inserting stack nodes */
    protected final ExecutorService workerPool;

    /** The instance used to group threads together */
    protected final ThreadGrouper threadGrouper;

    /** The interval to wait between sampling, in microseconds */
    protected final int interval;

    /** If sleeping threads should be ignored */
    private final boolean ignoreSleeping;

    /** If threads executing native code should be ignored */
    private final boolean ignoreNative;

    public AbstractDataAggregator(ExecutorService workerPool, ThreadGrouper threadGrouper, int interval, boolean ignoreSleeping, boolean ignoreNative) {
        this.workerPool = workerPool;
        this.threadGrouper = threadGrouper;
        this.interval = interval;
        this.ignoreSleeping = ignoreSleeping;
        this.ignoreNative = ignoreNative;
    }

    protected ThreadNode getNode(String group) {
        ThreadNode node = this.threadData.get(group); // fast path
        if (node != null) {
            return node;
        }
        return this.threadData.computeIfAbsent(group, ThreadNode::new);
    }

    protected void writeData(ThreadInfo threadInfo) {
        if (this.ignoreSleeping && isSleeping(threadInfo)) {
            return;
        }
        if (this.ignoreNative && threadInfo.isInNative()) {
            return;
        }

        try {
            ThreadNode node = getNode(this.threadGrouper.getGroup(threadInfo.getThreadId(), threadInfo.getThreadName()));
            node.log(threadInfo.getStackTrace(), this.interval);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isSleeping(ThreadInfo thread) {
        if (thread.getThreadState() == Thread.State.WAITING || thread.getThreadState() == Thread.State.TIMED_WAITING) {
            return true;
        }

        StackTraceElement[] stackTrace = thread.getStackTrace();
        if (stackTrace.length == 0) {
            return false;
        }

        StackTraceElement call = stackTrace[0];
        String clazz = call.getClassName();
        String method = call.getMethodName();

        // java.lang.Thread.yield()
        // jdk.internal.misc.Unsafe.park()
        // sun.misc.Unsafe.park()
        return (clazz.equals("java.lang.Thread") && method.equals("yield")) ||
                (clazz.equals("jdk.internal.misc.Unsafe") && method.equals("park")) ||
                (clazz.equals("sun.misc.Unsafe") && method.equals("park"));
    }

}
