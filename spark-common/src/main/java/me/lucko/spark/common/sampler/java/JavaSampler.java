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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.AbstractSampler;
import me.lucko.spark.common.sampler.SamplerMode;
import me.lucko.spark.common.sampler.SamplerSettings;
import me.lucko.spark.common.sampler.SamplerType;
import me.lucko.spark.common.sampler.window.ProfilingWindowUtils;
import me.lucko.spark.common.sampler.window.WindowStatisticsCollector;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.util.MethodDisambiguator;
import me.lucko.spark.common.util.SparkThreadFactory;
import me.lucko.spark.common.ws.ViewerSocket;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

/**
 * A sampler implementation using Java (WarmRoast).
 */
public class JavaSampler extends AbstractSampler implements Runnable {
    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);

    /** The worker pool for inserting stack nodes */
    private final ScheduledExecutorService workerPool = Executors.newScheduledThreadPool(
            6, new ThreadFactoryBuilder()
                    .setNameFormat("spark-java-sampler-" + THREAD_ID.getAndIncrement() + "-%d")
                    .setUncaughtExceptionHandler(SparkThreadFactory.EXCEPTION_HANDLER)
                    .build()
    );

    /** The main sampling task */
    private ScheduledFuture<?> task;

    /** The task to send statistics to the viewer socket */
    private ScheduledFuture<?> socketStatisticsTask;

    /** The thread management interface for the current JVM */
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    /** Responsible for aggregating and then outputting collected sampling data */
    private final JavaDataAggregator dataAggregator;

    /** The last window that was profiled */
    private final AtomicInteger lastWindow = new AtomicInteger();
    
    public JavaSampler(SparkPlatform platform, SamplerSettings settings) {
        super(platform, settings);
        this.dataAggregator = new SimpleJavaDataAggregator(this.workerPool, settings.threadGrouper(), settings.interval(), settings.ignoreSleeping());
    }

    public JavaSampler(SparkPlatform platform, SamplerSettings settings, TickHook tickHook, int tickLengthThreshold) {
        super(platform, settings);
        this.dataAggregator = new TickedJavaDataAggregator(this.workerPool, settings.threadGrouper(), settings.interval(), settings.ignoreSleeping(), tickHook, tickLengthThreshold);
    }

    @Override
    public void start() {
        super.start();

        TickHook tickHook = this.platform.getTickHook();
        if (tickHook != null) {
            if (this.dataAggregator instanceof TickedJavaDataAggregator) {
                WindowStatisticsCollector.ExplicitTickCounter counter = this.windowStatisticsCollector.startCountingTicksExplicit(tickHook);
                ((TickedJavaDataAggregator) this.dataAggregator).setTickCounter(counter);
            } else {
                this.windowStatisticsCollector.startCountingTicks(tickHook);
            }
        }

        this.windowStatisticsCollector.recordWindowStartTime(ProfilingWindowUtils.unixMillisToWindow(this.startTime));
        this.task = this.workerPool.scheduleAtFixedRate(this, 0, this.interval, TimeUnit.MICROSECONDS);
    }

    @Override
    public void stop(boolean cancelled) {
        super.stop(cancelled);

        this.task.cancel(false);

        if (this.socketStatisticsTask != null) {
            this.socketStatisticsTask.cancel(false);
        }

        if (!cancelled) {
            // collect statistics for the final window
            this.windowStatisticsCollector.measureNow(this.lastWindow.get());
        }

        this.workerPool.shutdown();
    }

    @Override
    public void run() {
        // this is effectively synchronized, the worker pool will not allow this task
        // to concurrently execute.
        try {
            long time = System.currentTimeMillis();

            if (this.autoEndTime != -1 && this.autoEndTime <= time) {
                stop(false);
                this.future.complete(this);
                return;
            }

            int window = ProfilingWindowUtils.unixMillisToWindow(time);
            ThreadInfo[] threadDumps = this.threadDumper.dumpThreads(this.threadBean);
            this.workerPool.execute(new InsertDataTask(threadDumps, window));
        } catch (Throwable t) {
            stop(false);
            this.future.completeExceptionally(t);
        }
    }

    @Override
    public void attachSocket(ViewerSocket socket) {
        super.attachSocket(socket);

        if (this.socketStatisticsTask == null) {
            this.socketStatisticsTask = this.workerPool.scheduleAtFixedRate(this::sendStatisticsToSocket, 10, 10, TimeUnit.SECONDS);
        }
    }

    private final class InsertDataTask implements Runnable {
        private final ThreadInfo[] threadDumps;
        private final int window;

        InsertDataTask(ThreadInfo[] threadDumps, int window) {
            this.threadDumps = threadDumps;
            this.window = window;
        }

        @Override
        public void run() {
            for (ThreadInfo threadInfo : this.threadDumps) {
                if (threadInfo.getThreadName() == null || threadInfo.getStackTrace() == null) {
                    continue;
                }
                JavaSampler.this.dataAggregator.insertData(threadInfo, this.window);
            }

            // if we have just stepped over into a new window...
            int previousWindow = JavaSampler.this.lastWindow.getAndUpdate(previous -> Math.max(this.window, previous));
            if (previousWindow != 0 && previousWindow != this.window) {

                // record the start time for the new window
                JavaSampler.this.windowStatisticsCollector.recordWindowStartTime(this.window);

                // collect statistics for the previous window
                JavaSampler.this.windowStatisticsCollector.measureNow(previousWindow);

                // prune data older than the history size
                IntPredicate predicate = ProfilingWindowUtils.keepHistoryBefore(this.window);
                JavaSampler.this.dataAggregator.pruneData(predicate);
                JavaSampler.this.windowStatisticsCollector.pruneStatistics(predicate);

                JavaSampler.this.workerPool.execute(JavaSampler.this::processWindowRotate);
            }
        }
    }

    @Override
    public SamplerData toProto(SparkPlatform platform, ExportProps exportProps) {
        SamplerData.Builder proto = SamplerData.newBuilder();
        if (exportProps.channelInfo() != null) {
            proto.setChannelInfo(exportProps.channelInfo());
        }

        writeMetadataToProto(proto, platform, exportProps.creator(), exportProps.comment(), this.dataAggregator);

        MethodDisambiguator methodDisambiguator = new MethodDisambiguator(platform.createClassFinder());
        writeDataToProto(proto, this.dataAggregator, timeEncoder -> new JavaNodeExporter(timeEncoder, exportProps.mergeStrategy(), methodDisambiguator), exportProps.classSourceLookup().get(), platform::createClassFinder);

        return proto.build();
    }

    @Override
    public SamplerType getType() {
        return SamplerType.JAVA;
    }

    @Override
    public String getLibraryVersion() {
        return null;
    }

    @Override
    public SamplerMode getMode() {
        return SamplerMode.EXECUTION;
    }
}
