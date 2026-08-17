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

package me.lucko.spark.common.sampler.java;

import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.aggregator.AbstractDataAggregator;
import me.lucko.spark.common.sampler.aggregator.DataAggregator;
import me.lucko.spark.common.sampler.node.StackTraceNode;
import me.lucko.spark.common.sampler.node.ThreadNode;

import java.lang.management.ThreadInfo;
import java.util.concurrent.ExecutorService;

/**
 * Abstract {@link DataAggregator} for the {@link JavaSampler}.
 */
public abstract class JavaDataAggregator extends AbstractDataAggregator {

    /** A describer for java.lang.StackTraceElement */
    private static final StackTraceNode.Describer<StackTraceElement> STACK_TRACE_DESCRIBER = (element, parent) -> {
        int parentLineNumber = parent == null ? StackTraceNode.NULL_LINE_NUMBER : parent.getLineNumber();
        return new StackTraceNode.JavaDescription(element.getClassName(), element.getMethodName(), element.getLineNumber(), parentLineNumber);
    };

    /** The worker pool for inserting stack nodes */
    protected final ExecutorService workerPool;

    /** The interval to wait between sampling, in microseconds */
    protected final int interval;

    public JavaDataAggregator(ExecutorService workerPool, ThreadGrouper threadGrouper, int interval, boolean ignoreSleeping) {
        super(threadGrouper, ignoreSleeping);
        this.workerPool = workerPool;
        this.interval = interval;
    }

    /**
     * Inserts sampling data into this aggregator
     *
     * @param threadInfo the thread info
     * @param window the window
     */
    public abstract void insertData(ThreadInfo threadInfo, int window);

    protected void writeData(ThreadInfo threadInfo, int window) {
        if (this.ignoreSleeping && isSleeping(threadInfo)) {
            return;
        }

        try {
            ThreadNode node = getNode(this.threadGrouper.getGroup(threadInfo.getThreadId(), threadInfo.getThreadName()));
            node.log(STACK_TRACE_DESCRIBER, threadInfo.getStackTrace(), this.interval, window);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean isSleeping(ThreadInfo thread) {
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

        return isSleeping(clazz, method);
    }

}
