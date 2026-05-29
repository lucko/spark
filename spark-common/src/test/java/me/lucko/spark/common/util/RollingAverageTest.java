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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RollingAverageTest {

    @Test
    public void testMean() {
        RollingAverage ra = new RollingAverage(3);
        ra.add(BigDecimal.valueOf(1));
        ra.add(BigDecimal.valueOf(2));
        ra.add(BigDecimal.valueOf(3));

        assertEquals(2, ra.mean());
        ra.add(BigDecimal.valueOf(4));
        assertEquals(3, ra.mean());
        ra.add(BigDecimal.valueOf(5));
        assertEquals(4, ra.mean());
        ra.add(BigDecimal.valueOf(6));
        assertEquals(5, ra.mean());
    }

    @Test
    public void testMax() {
        RollingAverage ra = new RollingAverage(3);
        ra.add(BigDecimal.valueOf(1));
        ra.add(BigDecimal.valueOf(2));
        ra.add(BigDecimal.valueOf(3));

        assertEquals(3, ra.max());
    }

    @Test
    public void testMin() {
        RollingAverage ra = new RollingAverage(3);
        ra.add(BigDecimal.valueOf(1));
        ra.add(BigDecimal.valueOf(2));
        ra.add(BigDecimal.valueOf(3));

        assertEquals(1, ra.min());
    }

    @Test
    public void testPercentile() {
        RollingAverage ra = new RollingAverage(3);
        ra.add(BigDecimal.valueOf(1));
        ra.add(BigDecimal.valueOf(2));
        ra.add(BigDecimal.valueOf(3));

        assertEquals(1, ra.percentile(0));
        assertEquals(2, ra.percentile(0.25));
        assertEquals(2, ra.percentile(0.5));
        assertEquals(3, ra.percentile(0.75));
        assertEquals(3, ra.percentile(1));
    }

}
