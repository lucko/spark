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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.sampler.AbstractSampler;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.util.ClassSourceLookup;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A sampler implementation using Java (WarmRoast).
 */
public class JavaSampler extends AbstractSampler implements Runnable {
    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);

    /** The worker pool for inserting stack nodes */
    private final ScheduledExecutorService workerPool = Executors.newScheduledThreadPool(
            6, new ThreadFactoryBuilder().setNameFormat("spark-worker-" + THREAD_ID.getAndIncrement() + "-%d").build()
    );

    /** The main sampling task */
    private ScheduledFuture<?> task;

    /** The thread management interface for the current JVM */
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    /** Responsible for aggregating and then outputting collected sampling data */
    private final JavaDataAggregator dataAggregator;
    
    public JavaSampler(int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper, long endTime, boolean ignoreSleeping, boolean ignoreNative) {
        super(interval, threadDumper, endTime);
        this.dataAggregator = new SimpleDataAggregator(this.workerPool, threadGrouper, interval, ignoreSleeping, ignoreNative);
    }

    public JavaSampler(int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper, long endTime, boolean ignoreSleeping, boolean ignoreNative, TickHook tickHook, int tickLengthThreshold) {
        super(interval, threadDumper, endTime);
        this.dataAggregator = new TickedDataAggregator(this.workerPool, threadGrouper, interval, ignoreSleeping, ignoreNative, tickHook, tickLengthThreshold);
    }

    @Override
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.task = this.workerPool.scheduleAtFixedRate(this, 0, this.interval, TimeUnit.MICROSECONDS);
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
    public SamplerData toProto(SparkPlatform platform, CommandSender creator, Comparator<? super Map.Entry<String, ThreadNode>> outputOrder, String comment, MergeMode mergeMode, ClassSourceLookup classSourceLookup) {
        SamplerData.Builder proto = SamplerData.newBuilder();
        writeMetadataToProto(proto, platform, creator, comment, this.dataAggregator);
        writeDataToProto(proto, this.dataAggregator, outputOrder, mergeMode, classSourceLookup);
        return proto.build();
    }

}
