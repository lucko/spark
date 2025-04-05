package me.lucko.spark.common.sampler.async;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceedingTicksFilterTest {

    @Test
    void testAggregateEmpty() {
        AtomicLong fakeNanos = new AtomicLong();
        ExceedingTicksFilter filter = new ExceedingTicksFilter(1, fakeNanos::get);
        assertEquals(0, filter.exceedingTicksCount());
        assertFalse(filter.duringExceedingTick(0));
    }

    @Test
    void testAggregateEmptyAfterTicks() {
        AtomicLong fakeNanos = new AtomicLong();
        ExceedingTicksFilter filter = new ExceedingTicksFilter(1, fakeNanos::get);
        tickWithDuration(filter, fakeNanos, 0);
        tickWithDuration(filter, fakeNanos, 500_000);
        tickWithDuration(filter, fakeNanos, 900_000);
        assertEquals(0, filter.exceedingTicksCount());
        assertFalse(filter.duringExceedingTick(0));
    }

    @Test
    void testAggregateOneExceeding() {
        AtomicLong fakeNanos = new AtomicLong();
        ExceedingTicksFilter filter = new ExceedingTicksFilter(1, fakeNanos::get);
        tickWithDuration(filter, fakeNanos, 500_000);
        long startOfExceeding = tickWithDuration(filter, fakeNanos, 1_500_000);
        tickWithDuration(filter, fakeNanos, 500_000);
        assertEquals(1, filter.exceedingTicksCount());
        assertFalse(filter.duringExceedingTick(startOfExceeding - 1));
        assertTrue(filter.duringExceedingTick(startOfExceeding + 1));
        assertTrue(filter.duringExceedingTick(startOfExceeding + 1_499_999));
        assertFalse(filter.duringExceedingTick(startOfExceeding + 1_500_001));
    }

    @Test
    void testAggregateMultipleExceeding() {
        AtomicLong fakeNanos = new AtomicLong();
        ExceedingTicksFilter filter = new ExceedingTicksFilter(1, fakeNanos::get);
        List<Long> starts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tickWithDuration(filter, fakeNanos, 500_000);
            long startOfExceeding = tickWithDuration(filter, fakeNanos, 1_500_000);
            starts.add(startOfExceeding);
            tickWithDuration(filter, fakeNanos, 500_000);
        }
        assertEquals(10, filter.exceedingTicksCount());
        for (long startOfExceeding : starts) {
            assertFalse(filter.duringExceedingTick(startOfExceeding - 1));
            assertTrue(filter.duringExceedingTick(startOfExceeding + 1));
            assertTrue(filter.duringExceedingTick(startOfExceeding + 1_499_999));
            assertFalse(filter.duringExceedingTick(startOfExceeding + 1_500_001));
        }
    }

    @Test
    void testAggregateDuringTicking() {
        AtomicLong fakeNanos = new AtomicLong();
        ExceedingTicksFilter filter = new ExceedingTicksFilter(1, fakeNanos::get);
        // no exceeding tick at time 1 yet
        assertFalse(filter.duringExceedingTick(1));
        tickWithDuration(filter, fakeNanos, 1_500_000);
        // tick exceeded at time 1 now
        assertTrue(filter.duringExceedingTick(1));
        // exceeded tick is still there
        assertTrue(filter.duringExceedingTick(1));
        // time after the exceeded tick
        assertFalse(filter.duringExceedingTick(1_500_001));
        // the exceeded tick was consumed now already
        assertFalse(filter.duringExceedingTick(1));
    }

    private static long tickWithDuration(ExceedingTicksFilter filter, AtomicLong fakeNanos, long durationNanos) {
        long before = fakeNanos.getAndAdd(durationNanos);
        filter.onTick(durationNanos / 1_000_000d);
        return before;
    }
}