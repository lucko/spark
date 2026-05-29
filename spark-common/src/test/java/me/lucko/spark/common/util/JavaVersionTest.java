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

public class JavaVersionTest {

    @Test
    public void testJavaVersion() {
        assertEquals(7, JavaVersion.parseJavaVersion("1.7"));
        assertEquals(8, JavaVersion.parseJavaVersion("1.8"));
        assertEquals(9, JavaVersion.parseJavaVersion("9"));
        assertEquals(11, JavaVersion.parseJavaVersion("11"));
        assertEquals(17, JavaVersion.parseJavaVersion("17"));
        assertEquals(9, JavaVersion.parseJavaVersion("9.0.1"));
        assertEquals(11, JavaVersion.parseJavaVersion("11.0.1"));
        assertEquals(17, JavaVersion.parseJavaVersion("17.0.1"));
        assertEquals(17, JavaVersion.parseJavaVersion("17-ea"));
        assertEquals(17, JavaVersion.parseJavaVersion("17.0.1-ea"));
    }

}
