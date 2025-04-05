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

    @Override
    public void onTick(double duration) {
        if (duration > tickLengthThreshold) {
            long end = this.nanoTimeSource.getAsLong();
            long start = (long) (end - (duration * 1_000_000)); // ms to ns
            this.ticksOver.add(new ExceededTick(start, end));
            this.tickCounter.getAndIncrement();
        }
    }

    int exceedingTicksCount() {
        return this.tickCounter.get();
    }

    boolean duringExceedingTick(long time) {
        while (true) {
            ExceededTick earliestExceeding = ticksOver.peek();
            if (earliestExceeding == null) {
                // no tick over threshold anymore
                return false;
            } else if (time - earliestExceeding.start < 0) {
                // segment happened before current exceeding
                return false;
            } else if (earliestExceeding.end - time < 0) {
                // segment happened after current exceeding,
                // but it might fall into the next one
                ticksOver.remove();
            } else {
                // segment falls exactly into exceeding, record it
                return true;
            }
        }
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
