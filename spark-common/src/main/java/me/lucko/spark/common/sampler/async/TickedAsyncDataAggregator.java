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
import me.lucko.spark.common.sampler.window.WindowStatisticsCollector;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.proto.SparkSamplerProtos;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class TickedAsyncDataAggregator extends AsyncDataAggregator {

    /** The ticks that exceeded the threshold, cleared one-by-one when inserting data */
    private final Queue<ExceededTick> ticksOver = new ConcurrentLinkedQueue<>();

    /** The callback called when this aggregator is closed, to clean up resources */
    private final Runnable closeCallback;

    /** Tick durations under this threshold will not be inserted, measured in microseconds */
    private final long tickLengthThreshold;

    /** Counts the number of ticks aggregated */
    private WindowStatisticsCollector.ExplicitTickCounter tickCounter;

    protected TickedAsyncDataAggregator(ThreadGrouper threadGrouper, boolean ignoreSleeping, TickReporter tickReporter, int tickLengthThreshold) {
        super(threadGrouper, ignoreSleeping);
        this.tickLengthThreshold = TimeUnit.MILLISECONDS.toMicros(tickLengthThreshold);
        TickReporter.Callback tickReporterCallback = duration -> {
            if (duration > tickLengthThreshold) {
                long end = System.nanoTime();
                long start = (long) (end - (duration * 1_000_000)); // ms to ns
                this.ticksOver.add(new ExceededTick(start, end));
                this.tickCounter.increment();
            }
        };
        tickReporter.addCallback(tickReporterCallback);
        this.closeCallback = () -> tickReporter.removeCallback(tickReporterCallback);
    }

    @Override
    public void insertData(ProfileSegment element, int window) {
        // with async-profiler clock=monotonic, the event time uses the same clock
        // as System.nanoTime(), so we can compare it directly
        long time = element.getTime();
        while (true) {
            ExceededTick earliestExceeding = ticksOver.peek();
            if (earliestExceeding == null) {
                // no tick over threshold anymore
                return;
            } else if (time - earliestExceeding.start < 0) {
                // segment happened before current exceeding
                return;
            } else if (earliestExceeding.end - time < 0) {
                // segment happened after current exceeding,
                // but it might fall into the next one
                ticksOver.remove();
            } else {
                // segment falls exactly into exceeding, record it
                break;
            }
        }
        super.insertData(element, window);
    }

    @Override
    public SparkSamplerProtos.SamplerMetadata.DataAggregator getMetadata() {
        return SparkSamplerProtos.SamplerMetadata.DataAggregator.newBuilder()
                .setType(SparkSamplerProtos.SamplerMetadata.DataAggregator.Type.TICKED)
                .setThreadGrouper(this.threadGrouper.asProto())
                .setTickLengthThreshold(this.tickLengthThreshold)
                .setNumberOfIncludedTicks(this.tickCounter.getTotalCountedTicks())
                .build();

    }

    @Override
    public void close() {
        this.closeCallback.run();
    }

    public void setTickCounter(WindowStatisticsCollector.ExplicitTickCounter counter) {
        this.tickCounter = counter;
    }

    private static final class ExceededTick {
        private final long start;
        private final long end;

        ExceededTick(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
}
