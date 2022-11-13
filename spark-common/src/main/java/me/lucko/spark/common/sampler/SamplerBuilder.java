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
import me.lucko.spark.common.sampler.java.JavaSampler;
import me.lucko.spark.common.tick.TickHook;

import java.util.concurrent.TimeUnit;

/**
 * Builds {@link Sampler} instances.
 */
@SuppressWarnings("UnusedReturnValue")
public class SamplerBuilder {

    private double samplingInterval = 4; // milliseconds
    private boolean ignoreSleeping = false;
    private boolean ignoreNative = false;
    private boolean useAsyncProfiler = true;
    private long autoEndTime = -1;
    private boolean background = false;
    private ThreadDumper threadDumper = ThreadDumper.ALL;
    private ThreadGrouper threadGrouper = ThreadGrouper.BY_NAME;

    private int ticksOver = -1;
    private TickHook tickHook = null;

    public SamplerBuilder() {
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

    public SamplerBuilder threadGrouper(ThreadGrouper threadGrouper) {
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

    public SamplerBuilder ignoreNative(boolean ignoreNative) {
        this.ignoreNative = ignoreNative;
        return this;
    }

    public SamplerBuilder forceJavaSampler(boolean forceJavaSampler) {
        this.useAsyncProfiler = !forceJavaSampler;
        return this;
    }

    public Sampler start(SparkPlatform platform) {
        boolean onlyTicksOverMode = this.ticksOver != -1 && this.tickHook != null;
        boolean canUseAsyncProfiler = this.useAsyncProfiler &&
                !onlyTicksOverMode &&
                !(this.ignoreSleeping || this.ignoreNative) &&
                !(this.threadDumper instanceof ThreadDumper.Regex) &&
                AsyncProfilerAccess.getInstance(platform).checkSupported(platform);


        int intervalMicros = (int) (this.samplingInterval * 1000d);
        SamplerSettings settings = new SamplerSettings(intervalMicros, this.threadDumper, this.threadGrouper, this.autoEndTime, this.background);

        Sampler sampler;
        if (canUseAsyncProfiler) {
            sampler = new AsyncSampler(platform, settings);
        } else if (onlyTicksOverMode) {
            sampler = new JavaSampler(platform, settings, this.ignoreSleeping, this.ignoreNative, this.tickHook, this.ticksOver);
        } else {
            sampler = new JavaSampler(platform, settings, this.ignoreSleeping, this.ignoreNative);
        }

        sampler.start();
        return sampler;
    }

}
