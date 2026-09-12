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

package me.lucko.spark.common.monitor.ping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PingSummaryTest {

    @Test
    public void testSimple() {
        PingSummary summary = new PingSummary(new int[]{7, 3, 10, 5, 1, 4});
        assertEquals(5d, summary.mean());
        assertEquals(10d, summary.max());
        assertEquals(1d, summary.min());
        assertEquals(5d, summary.median());
        assertEquals(10d, summary.percentile95th());
        assertEquals(30, summary.total());
    }

}
