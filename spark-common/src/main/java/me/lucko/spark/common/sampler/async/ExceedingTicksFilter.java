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

import me.lucko.spark.common.tick.TickReporter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

class ExceedingTicksFilter implements TickReporter.Callback {

    /** The ticks that exceeded the threshold, cleared one-by-one when inserting data */
    private final Queue<ExceededTick> ticksOver = new ConcurrentLinkedQueue<>();

    /** Counts the number of ticks aggregated */
    private final AtomicInteger tickCounter = new AtomicInteger();

    /** Tick durations under this threshold will not be inserted, measured in milliseconds */
    private final int tickLengthThreshold;

    /** The source to get the current nano time from */
    private final LongSupplier nanoTimeSource;

    ExceedingTicksFilter(int tickLengthThreshold, LongSupplier nanoTimeSource) {
        this.tickLengthThreshold = tickLengthThreshold;
        this.nanoTimeSource = nanoTimeSource;
    }

    public ExceedingTicksFilter(int tickLengthThreshold) {
        this(tickLengthThreshold, System::nanoTime);
    }

    @Override
    public void onTick(double duration) {
        if (duration > this.tickLengthThreshold) {
            long end = this.nanoTimeSource.getAsLong();
            long start = (long) (end - (duration * 1_000_000)); // ms to ns
            this.ticksOver.add(new ExceededTick(start, end));
            this.tickCounter.getAndIncrement();
        }
    }

    public int exceedingTicksCount() {
        return this.tickCounter.get();
    }

    public boolean duringExceedingTick(long time) {
        while (true) {
            ExceededTick earliestExceeding = this.ticksOver.peek();
            if (earliestExceeding == null) {
                // no tick over threshold anymore
                return false;
            } else if (time - earliestExceeding.start < 0) {
                // segment happened before current exceeding
                return false;
            } else if (earliestExceeding.end - time < 0) {
                // segment happened after current exceeding,
                // but it might fall into the next one
                this.ticksOver.remove();
            } else {
                // segment falls exactly into exceeding, record it
                return true;
            }
        }
    }

    private static final class ExceededTick {
        // times are in nanoseconds from System.nanoTime()
        private final long start;
        private final long end;

        ExceededTick(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
}
