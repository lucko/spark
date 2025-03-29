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

package me.lucko.spark.common.sampler.window;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProfilingWindowUtilsTest {

    @Test
    public void testMillisToWindow() {
        int baseWindow = 28532770;
        Instant baseTime = LocalDateTime.of(2024, Month.APRIL, 1, 10, 10, 0).toInstant(ZoneOffset.UTC);

        assertEquals(TimeUnit.MILLISECONDS.toMinutes(baseTime.toEpochMilli()), baseWindow); // should scale with unix time

        assertEquals(baseWindow, ProfilingWindowUtils.unixMillisToWindow(baseTime.toEpochMilli()));
        assertEquals(baseWindow, ProfilingWindowUtils.unixMillisToWindow(baseTime.plusMillis(1).toEpochMilli()));
        assertEquals(baseWindow, ProfilingWindowUtils.unixMillisToWindow(baseTime.plusSeconds(1).toEpochMilli()));
        assertEquals(baseWindow, ProfilingWindowUtils.unixMillisToWindow(baseTime.plusSeconds(59).toEpochMilli()));
        assertEquals(baseWindow + 1, ProfilingWindowUtils.unixMillisToWindow(baseTime.plusSeconds(60).toEpochMilli()));
        assertEquals(baseWindow + 1, ProfilingWindowUtils.unixMillisToWindow(baseTime.plusSeconds(61).toEpochMilli()));
        assertEquals(baseWindow - 1, ProfilingWindowUtils.unixMillisToWindow(baseTime.minusMillis(1).toEpochMilli()));
        assertEquals(baseWindow - 1, ProfilingWindowUtils.unixMillisToWindow(baseTime.minusSeconds(1).toEpochMilli()));
    }

    @Test
    public void testKeepHistoryBefore() {
        IntPredicate predicate = ProfilingWindowUtils.keepHistoryBefore(100);
        assertFalse(predicate.test(99));
        assertFalse(predicate.test(100));
        assertFalse(predicate.test(101));

        assertFalse(predicate.test(40));
        assertTrue(predicate.test(39));
        assertTrue(predicate.test(0));
        assertTrue(predicate.test(-10));
    }

}
