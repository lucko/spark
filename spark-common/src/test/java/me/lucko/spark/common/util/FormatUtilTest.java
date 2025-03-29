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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FormatUtilTest {

    @Test
    public void testPercent() {
        assertEquals("50%", FormatUtil.percent(0.5, 1));
        assertEquals("100%", FormatUtil.percent(1, 1));
        assertEquals("0%", FormatUtil.percent(0, 1));

        assertEquals("50%", FormatUtil.percent(50, 100));
        assertEquals("100%", FormatUtil.percent(100, 100));
        assertEquals("0%", FormatUtil.percent(0, 100));
    }

    @Test
    public void testBytes() {
        assertEquals("0 bytes", FormatUtil.formatBytes(0));
        assertEquals("1.0 bytes", FormatUtil.formatBytes(1));
        assertEquals("1.0 KB", FormatUtil.formatBytes(1024));
        assertEquals("1.0 MB", FormatUtil.formatBytes(1024 * 1024));
        assertEquals("1.0 GB", FormatUtil.formatBytes(1024 * 1024 * 1024));
        assertEquals("1.0 TB", FormatUtil.formatBytes(1024L * 1024 * 1024 * 1024));

        assertEquals("2.5 KB", FormatUtil.formatBytes((long) (1024 * 2.5d)));
        assertEquals("2.5 MB", FormatUtil.formatBytes((long) (1024 * 1024 * 2.5d)));
    }

    @Test
    public void testSeconds() {
        assertEquals("0s", FormatUtil.formatSeconds(0));
        assertEquals("1s", FormatUtil.formatSeconds(1));
        assertEquals("59s", FormatUtil.formatSeconds(59));
        assertEquals("1m", FormatUtil.formatSeconds(60));
        assertEquals("1m 1s", FormatUtil.formatSeconds(61));
        assertEquals("1m 59s", FormatUtil.formatSeconds(119));
        assertEquals("2m", FormatUtil.formatSeconds(120));
        assertEquals("2m 1s", FormatUtil.formatSeconds(121));
        assertEquals("2m 59s", FormatUtil.formatSeconds(179));
        assertEquals("3m", FormatUtil.formatSeconds(180));
    }

}
