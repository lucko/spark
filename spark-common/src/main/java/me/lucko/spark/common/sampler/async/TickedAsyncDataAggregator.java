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

import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.proto.SparkSamplerProtos;

import java.util.concurrent.TimeUnit;

public class TickedAsyncDataAggregator extends AsyncDataAggregator {

    /** The callback called when this aggregator is closed, to clean up resources */
    private final Runnable closeCallback;

    /** Tick durations under this threshold will not be inserted, measured in milliseconds */
    private final long tickLengthThreshold;

    private final ExceedingTicksFilter filter;

    protected TickedAsyncDataAggregator(ThreadGrouper threadGrouper, boolean ignoreSleeping, TickReporter tickReporter, int tickLengthThreshold) {
        super(threadGrouper, ignoreSleeping);
        this.tickLengthThreshold = TimeUnit.MILLISECONDS.toMicros(tickLengthThreshold);
        this.filter = new ExceedingTicksFilter(tickLengthThreshold);
        tickReporter.addCallback(this.filter);
        this.closeCallback = () -> tickReporter.removeCallback(this.filter);
    }

    @Override
    public void insertData(ProfileSegment element, int window) {
        // with async-profiler clock=monotonic, the event time uses the same clock
        // as System.nanoTime(), so we can compare it directly
        long time = element.getTime();
        if (!this.filter.duringExceedingTick(time)) {
            return;
        }
        super.insertData(element, window);
    }

    @Override
    public SparkSamplerProtos.SamplerMetadata.DataAggregator getMetadata() {
        return SparkSamplerProtos.SamplerMetadata.DataAggregator.newBuilder()
                .setType(SparkSamplerProtos.SamplerMetadata.DataAggregator.Type.TICKED)
                .setThreadGrouper(this.threadGrouper.asProto())
                .setTickLengthThreshold(this.tickLengthThreshold)
                .setNumberOfIncludedTicks(this.filter.exceedingTicksCount())
                .build();

    }

    @Override
    public void close() {
        this.closeCallback.run();
    }

}
