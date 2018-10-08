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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.stream.JsonWriter;

import me.lucko.spark.sampler.aggregator.DataAggregator;
import me.lucko.spark.sampler.aggregator.SimpleDataAggregator;
import me.lucko.spark.sampler.aggregator.TickedDataAggregator;
import me.lucko.spark.sampler.node.ThreadNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Main sampler class.
 */
public class Sampler implements Runnable {
    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);

    /** The worker pool for inserting stack nodes */
    private final ScheduledExecutorService workerPool = Executors.newScheduledThreadPool(
            9, new ThreadFactoryBuilder().setNameFormat("spark-worker-" + THREAD_ID.getAndIncrement() + "-%d").build()
    );

    /** The main sampling task */
    private ScheduledFuture<?> task;

    /** The thread management interface for the current JVM */
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    /** The instance used to generate thread information for use in sampling */
    private final ThreadDumper threadDumper;
    /** Responsible for aggregating and then outputting collected sampling data */
    private final DataAggregator dataAggregator;

    /** A future to encapsulation the completion of this sampler instance */
    private final CompletableFuture<Sampler> future = new CompletableFuture<>();

    /** The interval to wait between sampling, in milliseconds */
    private final int interval;
    /** The time when sampling first began */
    private long startTime = -1;
    /** The unix timestamp (in millis) when this sampler should automatically complete.*/
    private final long endTime; // -1 for nothing
    
    public Sampler(int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper, long endTime) {
        this.threadDumper = threadDumper;
        this.dataAggregator = new SimpleDataAggregator(this.workerPool, threadGrouper, interval);
        this.interval = interval;
        this.endTime = endTime;
    }

    public Sampler(int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper, long endTime, TickCounter tickCounter, int tickLengthThreshold) {
        this.threadDumper = threadDumper;
        this.dataAggregator = new TickedDataAggregator(this.workerPool, tickCounter, threadGrouper, interval, tickLengthThreshold);
        this.interval = interval;
        this.endTime = endTime;
    }

    /**
     * Starts the sampler.
     */
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.dataAggregator.start();
        this.task = workerPool.scheduleAtFixedRate(this, 0, interval, TimeUnit.MILLISECONDS);
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

    public void cancel() {
        task.cancel(false);
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

                this.dataAggregator.insertData(threadName, stack);
            }
        } catch (Throwable t) {
            this.future.completeExceptionally(t);
            cancel();
        }
    }

    private void writeOutput(JsonWriter writer) throws IOException {
        writer.beginObject();

        writer.name("type").value("sampler");
        writer.name("threads").beginArray();

        List<Map.Entry<String, ThreadNode>> data = new ArrayList<>(this.dataAggregator.getData().entrySet());
        data.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, ThreadNode> entry : data) {
            writer.beginObject();
            writer.name("threadName").value(entry.getKey());
            writer.name("totalTime").value(entry.getValue().getTotalTime());
            writer.name("rootNode");
            entry.getValue().serializeTo(writer);
            writer.endObject();
        }

        writer.endArray();
        writer.endObject();
    }

    public byte[] formCompressedDataPayload() {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(byteOut), StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = new JsonWriter(writer)) {
                writeOutput(jsonWriter);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteOut.toByteArray();
    }

}
