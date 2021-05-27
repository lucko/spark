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

package me.lucko.spark.common.sampler.java;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import me.lucko.spark.common.sampler.Sampler;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.util.ClassSourceLookup;
import me.lucko.spark.proto.SparkProtos.SamplerData;
import me.lucko.spark.proto.SparkProtos.SamplerMetadata;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A sampler implementation using Java (WarmRoast).
 */
public class JavaSampler implements Sampler, Runnable {
    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);

    /** The worker pool for inserting stack nodes */
    private final ScheduledExecutorService workerPool = Executors.newScheduledThreadPool(
            6, new ThreadFactoryBuilder().setNameFormat("spark-worker-" + THREAD_ID.getAndIncrement() + "-%d").build()
    );

    /** The main sampling task */
    private ScheduledFuture<?> task;

    /** The thread management interface for the current JVM */
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    /** The instance used to generate thread information for use in sampling */
    private final ThreadDumper threadDumper;
    /** Responsible for aggregating and then outputting collected sampling data */
    private final JavaDataAggregator dataAggregator;

    /** A future to encapsulate the completion of this sampler instance */
    private final CompletableFuture<JavaSampler> future = new CompletableFuture<>();
    /** The interval to wait between sampling, in microseconds */
    private final int interval;
    /** The time when sampling first began */
    private long startTime = -1;
    /** The unix timestamp (in millis) when this sampler should automatically complete. */
    private final long endTime; // -1 for nothing
    
    public JavaSampler(int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper, long endTime, boolean ignoreSleeping, boolean ignoreNative) {
        this.threadDumper = threadDumper;
        this.dataAggregator = new SimpleDataAggregator(this.workerPool, threadGrouper, interval, ignoreSleeping, ignoreNative);
        this.interval = interval;
        this.endTime = endTime;
    }

    public JavaSampler(int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper, long endTime, boolean ignoreSleeping, boolean ignoreNative, TickHook tickHook, int tickLengthThreshold) {
        this.threadDumper = threadDumper;
        this.dataAggregator = new TickedDataAggregator(this.workerPool, threadGrouper, interval, ignoreSleeping, ignoreNative, tickHook, tickLengthThreshold);
        this.interval = interval;
        this.endTime = endTime;
    }

    @Override
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.task = this.workerPool.scheduleAtFixedRate(this, 0, this.interval, TimeUnit.MICROSECONDS);
    }

    @Override
    public long getStartTime() {
        if (this.startTime == -1) {
            throw new IllegalStateException("Not yet started");
        }
        return this.startTime;
    }

    @Override
    public long getEndTime() {
        return this.endTime;
    }

    @Override
    public CompletableFuture<JavaSampler> getFuture() {
        return this.future;
    }

    @Override
    public void stop() {
        this.task.cancel(false);
    }

    @Override
    public void run() {
        // this is effectively synchronized, the worker pool will not allow this task
        // to concurrently execute.
        try {
            if (this.endTime != -1 && this.endTime <= System.currentTimeMillis()) {
                this.future.complete(this);
                stop();
                return;
            }

            ThreadInfo[] threadDumps = this.threadDumper.dumpThreads(this.threadBean);
            this.workerPool.execute(new InsertDataTask(this.dataAggregator, threadDumps));
        } catch (Throwable t) {
            this.future.completeExceptionally(t);
            stop();
        }
    }

    private static final class InsertDataTask implements Runnable {
        private final JavaDataAggregator dataAggregator;
        private final ThreadInfo[] threadDumps;

        InsertDataTask(JavaDataAggregator dataAggregator, ThreadInfo[] threadDumps) {
            this.dataAggregator = dataAggregator;
            this.threadDumps = threadDumps;
        }

        @Override
        public void run() {
            for (ThreadInfo threadInfo : this.threadDumps) {
                if (threadInfo.getThreadName() == null || threadInfo.getStackTrace() == null) {
                    continue;
                }
                this.dataAggregator.insertData(threadInfo);
            }
        }
    }

    @Override
    public SamplerData toProto(ExportProps props) {
        final SamplerMetadata.Builder metadata = SamplerMetadata.newBuilder()
                .setPlatformMetadata(props.platformInfo.toData().toProto())
                .setCreator(props.creator.toData().toProto())
                .setStartTime(this.startTime)
                .setInterval(this.interval)
                .setThreadDumper(this.threadDumper.getMetadata())
                .setDataAggregator(this.dataAggregator.getMetadata());

        if (props.comment != null) {
            metadata.setComment(props.comment);
        }

        SamplerData.Builder proto = SamplerData.newBuilder();
        proto.setMetadata(metadata.build());

        List<Map.Entry<String, ThreadNode>> data = new ArrayList<>(this.dataAggregator.getData().entrySet());
        data.sort(props.outputOrder);

        ClassSourceLookup.Visitor classSourceVisitor = ClassSourceLookup.createVisitor(props.classSourceLookup);

        for (Map.Entry<String, ThreadNode> entry : data) {
            proto.addThreads(entry.getValue().toProto(props.mergeMode));
            classSourceVisitor.visit(entry.getValue());
        }

        if (classSourceVisitor.hasMappings()) {
            proto.putAllClassSources(classSourceVisitor.getMapping());
        }

        return proto.build();
    }

}
