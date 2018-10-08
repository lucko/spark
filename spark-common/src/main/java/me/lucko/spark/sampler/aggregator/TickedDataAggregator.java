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
import me.lucko.spark.sampler.TickCounter;
import me.lucko.spark.sampler.node.AbstractNode;
import me.lucko.spark.sampler.node.ThreadNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link DataAggregator} which supports only including sampling data from "ticks"
 * which exceed a certain threshold in duration.
 */
public class TickedDataAggregator implements DataAggregator {

    /** A map of root stack nodes for each thread with sampling data */
    private final Map<String, ThreadNode> threadData = new ConcurrentHashMap<>();

    /** The worker pool for inserting stack nodes */
    private final ExecutorService workerPool;

    /** Used to monitor the current "tick" of the server */
    private final TickCounter tickCounter;

    /** The instance used to group threads together */
    private final ThreadGrouper threadGrouper;

    /** The interval to wait between sampling, in milliseconds */
    private final int interval;

    /** Tick durations under this threshold will not be inserted */
    private final int tickLengthThreshold;

    /** The expected number of samples in each tick */
    private final int expectedSize;

    private final Object mutex = new Object();

    // state
    private long currentTick = -1;
    private TickList currentData = new TickList(0);

    public TickedDataAggregator(ExecutorService workerPool, TickCounter tickCounter, ThreadGrouper threadGrouper, int interval, int tickLengthThreshold) {
        this.workerPool = workerPool;
        this.tickCounter = tickCounter;
        this.threadGrouper = threadGrouper;
        this.interval = interval;
        this.tickLengthThreshold = tickLengthThreshold;
        // 50 millis in a tick, plus 10 so we have a bit of room to go over
        this.expectedSize = (50 / interval) + 10;
    }

    @Override
    public void insertData(String threadName, StackTraceElement[] stack) {
        synchronized (this.mutex) {
            long tick = this.tickCounter.getCurrentTick();
            if (this.currentTick != tick) {
                pushCurrentTick();
                this.currentTick = tick;
                this.currentData = new TickList(this.expectedSize);
            }

            // form the queued data
            QueuedThreadInfo queuedData = new QueuedThreadInfo(threadName, stack);
            // insert it
            this.currentData.addData(queuedData);
        }
    }

    // guarded by 'mutex'
    private void pushCurrentTick() {
        TickList currentData = this.currentData;

        // approximate how long the tick lasted
        int tickLengthMillis = currentData.getList().size() * this.interval;

        // don't push data below the threshold
        if (tickLengthMillis < this.tickLengthThreshold) {
            return;
        }

        this.workerPool.submit(currentData);
    }

    @Override
    public void start() {
        this.tickCounter.start();
    }

    @Override
    public Map<String, ThreadNode> getData() {
        // push the current tick
        synchronized (this.mutex) {
            pushCurrentTick();
        }

        // close the tick counter
        this.tickCounter.close();

        // wait for all pending data to be inserted
        this.workerPool.shutdown();
        try {
            this.workerPool.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return this.threadData;
    }

    // called by TickList
    void insertData(List<QueuedThreadInfo> dataList) {
        for (QueuedThreadInfo data : dataList) {
            try {
                String group = this.threadGrouper.getGroup(data.threadName);
                AbstractNode node = this.threadData.computeIfAbsent(group, ThreadNode::new);
                node.log(data.stack, this.interval);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final class TickList implements Runnable {
        private final List<QueuedThreadInfo> list;

        TickList(int expectedSize) {
            this.list = new ArrayList<>(expectedSize);
        }

        @Override
        public void run() {
            insertData(this.list);
        }

        public List<QueuedThreadInfo> getList() {
            return this.list;
        }

        public void addData(QueuedThreadInfo data) {
            this.list.add(data);
        }
    }

    private static final class QueuedThreadInfo {
        private final String threadName;
        private final StackTraceElement[] stack;

        QueuedThreadInfo(String threadName, StackTraceElement[] stack) {
            this.threadName = threadName;
            this.stack = stack;
        }
    }
}
