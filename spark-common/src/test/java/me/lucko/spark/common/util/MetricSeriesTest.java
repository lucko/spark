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

package me.lucko.spark.common.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetricSeriesTest {

    @Test
    public void testEmpty() {
        MetricSeries<Double> series = new MetricSeries<>(Duration.ofMillis(10), 1);
        assertTrue(series.isEmpty());
        assertEquals(0, series.size());
        assertEquals(0, series.newestTimestamp());
        assertEquals(0, series.oldestTimestamp());
        series.forEach((timestamp, value) -> {
            throw new AssertionError("Should not be called");
        });

        MetricSeries.Export export = series.export();
        assertEquals(0, export.startTimestampMs());
        assertEquals(0, export.timestampDeltasMs().length);
        assertEquals(0, export.values().length);
    }

    @Test
    public void testAppend() {
        MetricSeries<Double> series = new MetricSeries<>(Duration.ofMillis(10), 1);
        series.record(1, 1.0);
        series.record(2, 2.0);

        assertEquals(2, series.size());
        assertFalse(series.isEmpty());
        assertEquals(2, series.newestTimestamp());
        assertEquals(1, series.oldestTimestamp());

        series.forEach((timestamp, value) -> {
            if (timestamp == 1) {
                assertEquals(1.0, value);
            } else if (timestamp == 2) {
                assertEquals(2.0, value);
            } else {
                throw new AssertionError("Unexpected timestamp: " + timestamp);
            }
        });

        MetricSeries.Export export = series.export();
        assertEquals(1, export.startTimestampMs());
        assertArrayEquals(new int[]{0, 1}, export.timestampDeltasMs());
        assertArrayEquals(new Object[]{1.0, 2.0}, export.values());
    }

    @Test
    public void testRetention() {
        MetricSeries<Double> series = new MetricSeries<>(Duration.ofMillis(10), 1);
        series.record(10, 1.0);
        series.record(20, 2.0);
        series.record(30, 3.0);

        assertEquals(30, series.newestTimestamp());
        assertEquals(20, series.oldestTimestamp());
        assertEquals(2, series.size());

        MetricSeries.Export export = series.export();
        assertEquals(20, export.startTimestampMs());
        assertArrayEquals(new int[]{0, 10}, export.timestampDeltasMs());
        assertArrayEquals(new Object[]{2.0, 3.0}, export.values());
    }

}
