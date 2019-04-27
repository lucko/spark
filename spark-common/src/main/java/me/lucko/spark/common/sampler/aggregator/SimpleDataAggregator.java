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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Basic implementation of {@link DataAggregator}.
 */
public class SimpleDataAggregator implements DataAggregator {

    /** A map of root stack nodes for each thread with sampling data */
    private final Map<String, ThreadNode> threadData = new ConcurrentHashMap<>();

    /** The worker pool used for sampling */
    private final ExecutorService workerPool;

    /** The instance used to group threads together */
    private final ThreadGrouper threadGrouper;

    /** The interval to wait between sampling, in microseconds */
    private final int interval;

    /** If line numbers should be included in the output */
    private final boolean includeLineNumbers;

    public SimpleDataAggregator(ExecutorService workerPool, ThreadGrouper threadGrouper, int interval, boolean includeLineNumbers) {
        this.workerPool = workerPool;
        this.threadGrouper = threadGrouper;
        this.interval = interval;
        this.includeLineNumbers = includeLineNumbers;
    }

    private ThreadNode getNode(String group) {
        ThreadNode node = this.threadData.get(group); // fast path
        if (node != null) {
            return node;
        }
        return this.threadData.computeIfAbsent(group, ThreadNode::new);
    }

    @Override
    public void insertData(long threadId, String threadName, StackTraceElement[] stack) {
        try {
            ThreadNode node = getNode(this.threadGrouper.getGroup(threadId, threadName));
            node.log(stack, this.interval, this.includeLineNumbers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, ThreadNode> getData() {
        // wait for all pending data to be inserted
        this.workerPool.shutdown();
        try {
            this.workerPool.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return this.threadData;
    }
}
