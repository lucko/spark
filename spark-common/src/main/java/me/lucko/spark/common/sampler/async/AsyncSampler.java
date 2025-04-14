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
import me.lucko.spark.common.sampler.AbstractSampler;
import me.lucko.spark.common.sampler.SamplerMode;
import me.lucko.spark.common.sampler.SamplerSettings;
import me.lucko.spark.common.sampler.SamplerType;
import me.lucko.spark.common.sampler.window.ProfilingWindowUtils;
import me.lucko.spark.common.sampler.window.WindowStatisticsCollector;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.util.SparkThreadFactory;
import me.lucko.spark.common.ws.ViewerSocket;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntPredicate;
import java.util.logging.Level;

/**
 * A sampler implementation using async-profiler.
 */
public class AsyncSampler extends AbstractSampler {

    /** Function to collect and measure samples - either execution or allocation */
    private final SampleCollector<?> sampleCollector;

    /** Object that provides access to the async-profiler API */
    private final AsyncProfilerAccess profilerAccess;

    /** Responsible for aggregating and then outputting collected sampling data */
    private final AsyncDataAggregator dataAggregator;

    /** Whether to force the sampler to use monotonic/nano time */
    private final boolean forceNanoTime;

    /** Mutex for the current profiler job */
    private final Object[] currentJobMutex = new Object[0];

    /** Current profiler job */
    private AsyncProfilerJob currentJob;

    /** The executor used for scheduling and management */
    private ScheduledExecutorService scheduler;

    /** The task to send statistics to the viewer socket */
    private ScheduledFuture<?> socketStatisticsTask;

    public AsyncSampler(SparkPlatform platform, SamplerSettings settings, SampleCollector<?> collector) {
        this(platform, settings, collector, new AsyncDataAggregator(settings.threadGrouper(), settings.ignoreSleeping()), false);
    }

    public AsyncSampler(SparkPlatform platform, SamplerSettings settings, SampleCollector<?> collector, int tickLengthThreshold) {
        this(platform, settings, collector, new TickedAsyncDataAggregator(settings.threadGrouper(), settings.ignoreSleeping(), platform.getTickReporter(), tickLengthThreshold), true);
    }

    private AsyncSampler(SparkPlatform platform, SamplerSettings settings, SampleCollector<?> collector, AsyncDataAggregator dataAggregator, boolean forceNanoTime) {
        super(platform, settings);
        this.sampleCollector = collector;
        this.dataAggregator = dataAggregator;
        this.forceNanoTime = forceNanoTime;
        this.profilerAccess = AsyncProfilerAccess.getInstance(platform);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("spark-async-sampler-worker-thread")
                        .setUncaughtExceptionHandler(SparkThreadFactory.EXCEPTION_HANDLER)
                        .build()
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
        job.init(this.platform, this.sampleCollector, this.threadDumper, window, this.background, this.forceNanoTime);
        job.start();
        this.windowStatisticsCollector.recordWindowStartTime(window);
        this.currentJob = job;

        // rotate the sampler job to put data into a new window
        boolean shouldNotRotate = this.sampleCollector instanceof SampleCollector.Allocation && ((SampleCollector.Allocation) this.sampleCollector).isLiveOnly();
        if (!shouldNotRotate) {
            this.scheduler.scheduleAtFixedRate(
                    this::rotateProfilerJob,
                    ProfilingWindowUtils.WINDOW_SIZE_SECONDS,
                    ProfilingWindowUtils.WINDOW_SIZE_SECONDS,
                    TimeUnit.SECONDS
            );
        }

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
                } catch (Exception e) {
                    this.platform.getPlugin().log(Level.WARNING, "Failed to stop previous profiler job", e);
                }

                // start a new job
                int window = previousJob.getWindow() + 1;
                AsyncProfilerJob newJob = this.profilerAccess.startNewProfilerJob();
                newJob.init(this.platform, this.sampleCollector, this.threadDumper, window, this.background, this.forceNanoTime);
                newJob.start();
                this.windowStatisticsCollector.recordWindowStartTime(window);
                this.currentJob = newJob;

                // collect statistics for the previous window
                try {
                    this.windowStatisticsCollector.measureNow(previousJob.getWindow());
                } catch (Exception e) {
                    this.platform.getPlugin().log(Level.WARNING, "Failed to measure window statistics", e);
                }

                // aggregate the output of the previous job
                previousJob.aggregate(this.dataAggregator);

                // prune data older than the history size
                IntPredicate predicate = ProfilingWindowUtils.keepHistoryBefore(window);
                this.dataAggregator.pruneData(predicate);
                this.windowStatisticsCollector.pruneStatistics(predicate);

                this.scheduler.execute(this::processWindowRotate);
            }
        } catch (Throwable e) {
            this.platform.getPlugin().log(Level.WARNING, "Exception occurred while rotating profiler job", e);
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
            try {
                stop(false);
                this.future.complete(this);
            } catch (Exception e) {
                this.future.completeExceptionally(e);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the profiler.
     */
    @Override
    public void stop(boolean cancelled) {
        super.stop(cancelled);

        synchronized (this.currentJobMutex) {
            this.currentJob.stop();
            if (!cancelled) {
                this.windowStatisticsCollector.measureNow(this.currentJob.getWindow());
                this.currentJob.aggregate(this.dataAggregator);
            } else {
                this.currentJob.deleteOutputFile();
            }
            this.currentJob = null;
        }

        if (this.socketStatisticsTask != null) {
            this.socketStatisticsTask.cancel(false);
        }

        if (this.scheduler != null) {
            this.scheduler.shutdown();
            this.scheduler = null;
        }
        this.dataAggregator.close();
    }

    @Override
    public void attachSocket(ViewerSocket socket) {
        super.attachSocket(socket);

        if (this.socketStatisticsTask == null) {
            this.socketStatisticsTask = this.scheduler.scheduleAtFixedRate(this::sendStatisticsToSocket, 10, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public SamplerType getType() {
        return SamplerType.ASYNC;
    }

    @Override
    public String getLibraryVersion() {
        return this.profilerAccess.getVersion();
    }

    @Override
    public SamplerMode getMode() {
        return this.sampleCollector.getMode();
    }

    @Override
    public SamplerData toProto(SparkPlatform platform, ExportProps exportProps) {
        SamplerData.Builder proto = SamplerData.newBuilder();
        if (exportProps.channelInfo() != null) {
            proto.setChannelInfo(exportProps.channelInfo());
        }
        writeMetadataToProto(proto, platform, exportProps.creator(), exportProps.comment(), this.dataAggregator);
        writeDataToProto(proto, this.dataAggregator, AsyncNodeExporter::new, exportProps.classSourceLookup().get(), platform::createClassFinder);
        return proto.build();
    }

}
