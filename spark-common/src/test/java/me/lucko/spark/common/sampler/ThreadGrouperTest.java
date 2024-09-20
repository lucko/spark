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

package me.lucko.spark.common.sampler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreadGrouperTest {

    @Test
    public void testByName() {
        ThreadGrouper threadGrouper = ThreadGrouper.BY_NAME.get();

        String group = threadGrouper.getGroup(1, "main");
        assertEquals("main", group);

        String label = threadGrouper.getLabel("main");
        assertEquals("main", label);
    }

    @Test
    public void testAsOne() {
        ThreadGrouper threadGrouper = ThreadGrouper.AS_ONE.get();

        String group = threadGrouper.getGroup(1, "main");
        assertEquals("root", group);

        String label = threadGrouper.getLabel("root");
        assertEquals("All (x1)", label);

        group = threadGrouper.getGroup(2, "main2");
        assertEquals("root", group);

        label = threadGrouper.getLabel("root");
        assertEquals("All (x2)", label);
    }

    @Test
    public void testByPool() {
        ThreadGrouper threadGrouper = ThreadGrouper.BY_POOL.get();

        String group = threadGrouper.getGroup(1, "main");
        assertEquals("main", group);

        String label = threadGrouper.getLabel("main");
        assertEquals("main", label);

        group = threadGrouper.getGroup(2, "Test Pool - #1");
        assertEquals("Test Pool", group);

        label = threadGrouper.getLabel("Test Pool");
        assertEquals("Test Pool (x1)", label);

        group = threadGrouper.getGroup(3, "Test Pool - #2");
        assertEquals("Test Pool", group);

        label = threadGrouper.getLabel("Test Pool");
        assertEquals("Test Pool (x2)", label);
    }

}
