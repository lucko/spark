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

import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.aggregator.DataAggregator;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.common.sampler.window.WindowStatisticsCollector;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link DataAggregator} which supports only including sampling data from "ticks"
 * which exceed a certain threshold in duration.
 */
public class TickedDataAggregator extends JavaDataAggregator {

    /** Used to monitor the current "tick" of the server */
    private final TickHook tickHook;

    /** Tick durations under this threshold will not be inserted, measured in microseconds */
    private final long tickLengthThreshold;

    /** The expected number of samples in each tick */
    private final int expectedSize;

    /** Counts the number of ticks aggregated */
    private WindowStatisticsCollector.ExplicitTickCounter tickCounter;

    // state
    private int currentTick = -1;
    private TickList currentData = null;

    // guards currentData
    private final Object mutex = new Object();

    public TickedDataAggregator(ExecutorService workerPool, ThreadGrouper threadGrouper, int interval, boolean ignoreSleeping, boolean ignoreNative, TickHook tickHook, int tickLengthThreshold) {
        super(workerPool, threadGrouper, interval, ignoreSleeping, ignoreNative);
        this.tickHook = tickHook;
        this.tickLengthThreshold = TimeUnit.MILLISECONDS.toMicros(tickLengthThreshold);
        // 50 millis in a tick, plus 10 so we have a bit of room to go over
        double intervalMilliseconds = interval / 1000d;
        this.expectedSize = (int) ((50 / intervalMilliseconds) + 10);
    }

    public void setTickCounter(WindowStatisticsCollector.ExplicitTickCounter tickCounter) {
        this.tickCounter = tickCounter;
    }

    @Override
    public SamplerMetadata.DataAggregator getMetadata() {
        // push the current tick (so numberOfTicks is accurate)
        synchronized (this.mutex) {
            pushCurrentTick(Runnable::run);
            this.currentData = null;
        }

        return SamplerMetadata.DataAggregator.newBuilder()
                .setType(SamplerMetadata.DataAggregator.Type.TICKED)
                .setThreadGrouper(this.threadGrouper.asProto())
                .setTickLengthThreshold(this.tickLengthThreshold)
                .setNumberOfIncludedTicks(this.tickCounter.getTotalCountedTicks())
                .build();
    }

    @Override
    public void insertData(ThreadInfo threadInfo, int window) {
        synchronized (this.mutex) {
            int tick = this.tickHook.getCurrentTick();
            if (this.currentTick != tick || this.currentData == null) {
                pushCurrentTick(this.workerPool);
                this.currentTick = tick;
                this.currentData = new TickList(this.expectedSize, window);
            }

            this.currentData.addData(threadInfo);
        }
    }

    // guarded by 'mutex'
    private void pushCurrentTick(Executor executor) {
        TickList currentData = this.currentData;
        if (currentData == null) {
            return;
        }

        // approximate how long the tick lasted
        int tickLengthMicros = currentData.getList().size() * this.interval;

        // don't push data below the threshold
        if (tickLengthMicros < this.tickLengthThreshold) {
            return;
        }

        executor.execute(currentData);
        this.tickCounter.increment();
    }

    @Override
    public List<ThreadNode> exportData() {
        // push the current tick
        synchronized (this.mutex) {
            pushCurrentTick(Runnable::run);
        }

        return super.exportData();
    }

    private final class TickList implements Runnable {
        private final List<ThreadInfo> list;
        private final int window;

        TickList(int expectedSize, int window) {
            this.list = new ArrayList<>(expectedSize);
            this.window = window;
        }

        @Override
        public void run() {
            for (ThreadInfo data : this.list) {
                writeData(data, this.window);
            }
        }

        public List<ThreadInfo> getList() {
            return this.list;
        }

        public void addData(ThreadInfo data) {
            this.list.add(data);
        }
    }
}
