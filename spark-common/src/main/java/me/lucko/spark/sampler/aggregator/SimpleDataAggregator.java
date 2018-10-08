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

package me.lucko.spark.sampler.aggregator;

import me.lucko.spark.sampler.ThreadGrouper;
import me.lucko.spark.sampler.node.AbstractNode;
import me.lucko.spark.sampler.node.ThreadNode;

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

    /** The interval to wait between sampling, in milliseconds */
    private final int interval;

    public SimpleDataAggregator(ExecutorService workerPool, ThreadGrouper threadGrouper, int interval) {
        this.workerPool = workerPool;
        this.threadGrouper = threadGrouper;
        this.interval = interval;
    }

    @Override
    public void insertData(String threadName, StackTraceElement[] stack) {
        try {
            String group = this.threadGrouper.getGroup(threadName);
            AbstractNode node = this.threadData.computeIfAbsent(group, ThreadNode::new);
            node.log(stack, this.interval);
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
