/*
 * WarmRoast
 * Copyright (C) 2013 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package me.lucko.spark.profiler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main sampler class.
 */
public class Sampler extends TimerTask {
    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);

    /** The thread management interface for the current JVM */
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    /** A map of root stack nodes for each thread with sampling data */
    private final Map<String, StackNode> threadData = new ConcurrentHashMap<>();

    /** The worker pool for inserting stack nodes */
    private final ExecutorService workerPool = Executors.newFixedThreadPool(
            6, new ThreadFactoryBuilder().setNameFormat("spark-worker-" + THREAD_ID.getAndIncrement()).build()
    );

    /** A future to encapsulation the completion of this sampler instance */
    private final CompletableFuture<Sampler> future = new CompletableFuture<>();

    /** The interval to wait between sampling, in milliseconds */
    private final int interval;
    /** The instance used to generate thread information for use in sampling */
    private final ThreadDumper threadDumper;
    /** The instance used to group threads together */
    private final ThreadGrouper threadGrouper;
    /** The time when sampling first began */
    private long startTime = -1;
    /** The unix timestamp (in millis) when this sampler should automatically complete.*/
    private final long endTime; // -1 for nothing
    
    public Sampler(int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper, long endTime) {
        this.interval = interval;
        this.threadDumper = threadDumper;
        this.threadGrouper = threadGrouper;
        this.endTime = endTime;
    }

    /**
     * Starts the sampler.
     *
     * @param samplingThread the timer to schedule the sampling on
     */
    public void start(Timer samplingThread) {
        samplingThread.scheduleAtFixedRate(this, 0, this.interval);
        this.startTime = System.currentTimeMillis();
    }

    private void insertData(QueuedThreadInfo data) {
        try {
            String group = this.threadGrouper.getGroup(data.threadName);
            StackNode node = this.threadData.computeIfAbsent(group, StackNode::new);
            node.log(data.stack, Sampler.this.interval);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the sampling data recorded by this instance.
     *
     * @return the data
     */
    public Map<String, StackNode> getData() {
        // wait for all pending data to be inserted
        this.workerPool.shutdown();
        try {
            this.workerPool.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return this.threadData;
    }

    public long getStartTime() {
        if (this.startTime == -1) {
            throw new IllegalStateException("Not yet started");
        }
        return this.startTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public CompletableFuture<Sampler> getFuture() {
        return this.future;
    }

    @Override
    public void run() {
        try {
            if (this.endTime != -1 && this.endTime <= System.currentTimeMillis()) {
                this.future.complete(this);
                cancel();
                return;
            }

            ThreadInfo[] threadDumps = this.threadDumper.dumpThreads(this.threadBean);
            for (ThreadInfo threadInfo : threadDumps) {
                String threadName = threadInfo.getThreadName();
                StackTraceElement[] stack = threadInfo.getStackTrace();

                if (threadName == null || stack == null) {
                    continue;
                }

                // form the queued data
                QueuedThreadInfo queuedData = new QueuedThreadInfo(threadName, stack);
                // schedule insertion of the data
                this.workerPool.execute(queuedData);
            }
        } catch (Throwable t) {
            this.future.completeExceptionally(t);
            cancel();
        }
    }

    public JsonObject formOutput() {
        JsonObject out = new JsonObject();

        JsonArray threads = new JsonArray();

        List<Map.Entry<String, StackNode>> data = new ArrayList<>(getData().entrySet());
        data.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, StackNode> entry : data) {
            JsonObject o = new JsonObject();
            o.addProperty("threadName", entry.getKey());
            o.addProperty("totalTime", entry.getValue().getTotalTime());
            o.add("rootNode", entry.getValue().serialize());

            threads.add(o);
        }
        out.add("threads", threads);

        return out;
    }

    private final class QueuedThreadInfo implements Runnable {
        private final String threadName;
        private final StackTraceElement[] stack;

        private QueuedThreadInfo(String threadName, StackTraceElement[] stack) {
            this.threadName = threadName;
            this.stack = stack;
        }

        @Override
        public void run() {
            insertData(this);
        }
    }

}
