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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExceedingTicksFilterTest {

    @Test
    public void testAggregateEmpty() {
        AtomicLong fakeNanos = new AtomicLong();
        ExceedingTicksFilter filter = new ExceedingTicksFilter(1, fakeNanos::get);
        assertEquals(0, filter.exceedingTicksCount());
        assertFalse(filter.duringExceedingTick(0));
    }

    @Test
    public void testAggregateEmptyAfterTicks() {
        AtomicLong fakeNanos = new AtomicLong();
        ExceedingTicksFilter filter = new ExceedingTicksFilter(1, fakeNanos::get);
        tickWithDuration(filter, fakeNanos, 0);
        tickWithDuration(filter, fakeNanos, 500_000); // 0.5 ms
        tickWithDuration(filter, fakeNanos, 900_000); // 0.9 ms
        assertEquals(0, filter.exceedingTicksCount());
        assertFalse(filter.duringExceedingTick(0));
    }

    @Test
    public void testAggregateOneExceeding() {
        AtomicLong fakeNanos = new AtomicLong();
        ExceedingTicksFilter filter = new ExceedingTicksFilter(1, fakeNanos::get);
        tickWithDuration(filter, fakeNanos, 500_000); // 0.5 ms
        long startOfExceeding = tickWithDuration(filter, fakeNanos, 1_500_000); // 1.5 ms
        tickWithDuration(filter, fakeNanos, 500_000); // 0.5 ms
        assertEquals(1, filter.exceedingTicksCount());
        assertFalse(filter.duringExceedingTick(startOfExceeding - 1));
        assertTrue(filter.duringExceedingTick(startOfExceeding));
        assertTrue(filter.duringExceedingTick(startOfExceeding + 1));
        assertTrue(filter.duringExceedingTick(startOfExceeding + 1_499_999));
        assertTrue(filter.duringExceedingTick(startOfExceeding + 1_500_000));
        assertFalse(filter.duringExceedingTick(startOfExceeding + 1_500_001));
    }

    @Test
    public void testAggregateMultipleExceeding() {
        AtomicLong fakeNanos = new AtomicLong();
        ExceedingTicksFilter filter = new ExceedingTicksFilter(1, fakeNanos::get);
        List<Long> starts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tickWithDuration(filter, fakeNanos, 500_000); // 0.5 ms
            long startOfExceeding = tickWithDuration(filter, fakeNanos, 1_500_000); // 1.5 ms
            starts.add(startOfExceeding);
            tickWithDuration(filter, fakeNanos, 500_000); // 0.5 ms
        }
        assertEquals(10, filter.exceedingTicksCount());
        for (long startOfExceeding : starts) {
            assertFalse(filter.duringExceedingTick(startOfExceeding - 1));
            assertTrue(filter.duringExceedingTick(startOfExceeding + 1));
            assertTrue(filter.duringExceedingTick(startOfExceeding));
            assertTrue(filter.duringExceedingTick(startOfExceeding + 1_499_999));
            assertTrue(filter.duringExceedingTick(startOfExceeding + 1_500_000));
            assertFalse(filter.duringExceedingTick(startOfExceeding + 1_500_001));
        }
    }

    @Test
    public void testAggregateDuringTicking() {
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