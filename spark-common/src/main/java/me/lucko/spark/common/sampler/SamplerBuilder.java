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

package me.lucko.spark.common.sampler;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.async.AsyncProfilerAccess;
import me.lucko.spark.common.sampler.async.AsyncSampler;
import me.lucko.spark.common.sampler.async.SampleCollector;
import me.lucko.spark.common.sampler.java.JavaSampler;
import me.lucko.spark.common.tick.TickHook;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Builds {@link Sampler} instances.
 */
@SuppressWarnings("UnusedReturnValue")
public class SamplerBuilder {

    private SamplerMode mode = SamplerMode.EXECUTION;
    private double samplingInterval = -1;
    private boolean ignoreSleeping = false;
    private boolean forceJavaSampler = false;
    private boolean allocLiveOnly = false;
    private long autoEndTime = -1;
    private boolean background = false;
    private ThreadDumper threadDumper = ThreadDumper.ALL;
    private Supplier<ThreadGrouper> threadGrouper = ThreadGrouper.BY_NAME;

    private int ticksOver = -1;
    private TickHook tickHook = null;

    public SamplerBuilder() {
    }

    public SamplerBuilder mode(SamplerMode mode) {
        this.mode = mode;
        return this;
    }

    public SamplerBuilder samplingInterval(double samplingInterval) {
        this.samplingInterval = samplingInterval;
        return this;
    }

    public SamplerBuilder completeAfter(long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout > 0");
        }
        this.autoEndTime = System.currentTimeMillis() + unit.toMillis(timeout);
        return this;
    }

    public SamplerBuilder background(boolean background) {
        this.background = background;
        return this;
    }

    public SamplerBuilder threadDumper(ThreadDumper threadDumper) {
        this.threadDumper = threadDumper;
        return this;
    }

    public SamplerBuilder threadGrouper(Supplier<ThreadGrouper> threadGrouper) {
        this.threadGrouper = threadGrouper;
        return this;
    }

    public SamplerBuilder ticksOver(int ticksOver, TickHook tickHook) {
        this.ticksOver = ticksOver;
        this.tickHook = tickHook;
        return this;
    }

    public SamplerBuilder ignoreSleeping(boolean ignoreSleeping) {
        this.ignoreSleeping = ignoreSleeping;
        return this;
    }

    public SamplerBuilder forceJavaSampler(boolean forceJavaSampler) {
        this.forceJavaSampler = forceJavaSampler;
        return this;
    }

    public SamplerBuilder allocLiveOnly(boolean allocLiveOnly) {
        this.allocLiveOnly = allocLiveOnly;
        return this;
    }

    public Sampler start(SparkPlatform platform) throws UnsupportedOperationException {
        if (this.samplingInterval <= 0) {
            throw new IllegalArgumentException("samplingInterval = " + this.samplingInterval);
        }

        AsyncProfilerAccess asyncProfiler = AsyncProfilerAccess.getInstance(platform);

        boolean onlyTicksOverMode = this.ticksOver != -1 && this.tickHook != null;
        boolean canUseAsyncProfiler = asyncProfiler.checkSupported(platform) && (!onlyTicksOverMode || platform.getTickReporter() != null);

        if (this.mode == SamplerMode.ALLOCATION) {
            if (!canUseAsyncProfiler || !asyncProfiler.checkAllocationProfilingSupported(platform)) {
                throw new UnsupportedOperationException("Allocation profiling is not supported on your system. Check the console for more info.");
            }
            if (this.ignoreSleeping) {
                platform.getPlugin().log(Level.WARNING, "Ignoring sleeping threads is not supported in allocation profiling mode. Sleeping threads will be included in the results.");
            }
        }

        if (this.forceJavaSampler) {
            canUseAsyncProfiler = false;
        }

        int interval = (int) (this.mode == SamplerMode.EXECUTION ?
                this.samplingInterval * 1000d : // convert to microseconds
                this.samplingInterval
        );

        SamplerSettings settings = new SamplerSettings(interval, this.threadDumper, this.threadGrouper.get(), this.autoEndTime, this.background, this.ignoreSleeping);

        Sampler sampler;
        if (canUseAsyncProfiler) {
            SampleCollector<?> collector = this.mode == SamplerMode.ALLOCATION
                    ? new SampleCollector.Allocation(interval, this.allocLiveOnly)
                    : new SampleCollector.Execution(interval);
            sampler = onlyTicksOverMode
                    ? new AsyncSampler(platform, settings, collector, this.ticksOver)
                    : new AsyncSampler(platform, settings, collector);
        } else {
            sampler = onlyTicksOverMode
                    ? new JavaSampler(platform, settings, this.tickHook, this.ticksOver)
                    : new JavaSampler(platform, settings);
        }

        sampler.start();
        return sampler;
    }

}
