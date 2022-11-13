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

package me.lucko.spark.common.sampler.async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.sampler.AbstractSampler;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.window.ProfilingWindowUtils;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A sampler implementation using async-profiler.
 */
public class AsyncSampler extends AbstractSampler {
    private final AsyncProfilerAccess profilerAccess;

    /** Responsible for aggregating and then outputting collected sampling data */
    private final AsyncDataAggregator dataAggregator;

    /** Mutex for the current profiler job */
    private final Object[] currentJobMutex = new Object[0];

    /** Current profiler job */
    private AsyncProfilerJob currentJob;

    /** The executor used for scheduling and management */
    private ScheduledExecutorService scheduler;

    public AsyncSampler(SparkPlatform platform, int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper, long endTime) {
        super(platform, interval, threadDumper, endTime);
        this.profilerAccess = AsyncProfilerAccess.getInstance(platform);
        this.dataAggregator = new AsyncDataAggregator(threadGrouper);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("spark-asyncsampler-worker-thread").build()
        );
    }

    /**
     * Starts the profiler.
     */
    @Override
    public void start() {
        super.start();

        TickHook tickHook = this.platform.getTickHook();
        if (tickHook != null) {
            this.windowStatisticsCollector.startCountingTicks(tickHook);
        }

        int window = ProfilingWindowUtils.windowNow();

        AsyncProfilerJob job = this.profilerAccess.startNewProfilerJob();
        job.init(this.platform, this.interval, this.threadDumper, window);
        job.start();
        this.currentJob = job;

        // rotate the sampler job to put data into a new window
        this.scheduler.scheduleAtFixedRate(
                this::rotateProfilerJob,
                ProfilingWindowUtils.WINDOW_SIZE_SECONDS,
                ProfilingWindowUtils.WINDOW_SIZE_SECONDS,
                TimeUnit.SECONDS
        );

        recordInitialGcStats();
        scheduleTimeout();
    }

    private void rotateProfilerJob() {
        try {
            synchronized (this.currentJobMutex) {
                AsyncProfilerJob previousJob = this.currentJob;
                if (previousJob == null) {
                    return;
                }

                try {
                    // stop the previous job
                    previousJob.stop();

                    // collect statistics for the window
                    this.windowStatisticsCollector.measureNow(previousJob.getWindow());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // start a new job
                int window = previousJob.getWindow() + 1;
                AsyncProfilerJob newJob = this.profilerAccess.startNewProfilerJob();
                newJob.init(this.platform, this.interval, this.threadDumper, window);
                newJob.start();
                this.currentJob = newJob;

                // aggregate the output of the previous job
                previousJob.aggregate(this.dataAggregator);

                // prune data older than the history size
                this.dataAggregator.pruneData(ProfilingWindowUtils.keepHistoryBefore(window));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void scheduleTimeout() {
        if (this.autoEndTime == -1) {
            return;
        }

        long delay = this.autoEndTime - System.currentTimeMillis();
        if (delay <= 0) {
            return;
        }

        this.scheduler.schedule(() -> {
            stop();
            this.future.complete(this);
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the profiler.
     */
    @Override
    public void stop() {
        super.stop();

        synchronized (this.currentJobMutex) {
            this.currentJob.stop();
            this.windowStatisticsCollector.measureNow(this.currentJob.getWindow());
            this.currentJob.aggregate(this.dataAggregator);
            this.currentJob = null;
        }

        if (this.scheduler != null) {
            this.scheduler.shutdown();
            this.scheduler = null;
        }
    }

    @Override
    public SamplerData toProto(SparkPlatform platform, CommandSender creator, String comment, MergeMode mergeMode, ClassSourceLookup classSourceLookup) {
        SamplerData.Builder proto = SamplerData.newBuilder();
        writeMetadataToProto(proto, platform, creator, comment, this.dataAggregator);
        writeDataToProto(proto, this.dataAggregator, mergeMode, classSourceLookup);
        return proto.build();
    }

}
