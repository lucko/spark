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

package me.lucko.spark.common.heapdump;

import me.lucko.spark.test.TestClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HeapDumpSummaryTest {

    @Test
    public void testHeapDumpSummary() throws Exception {
        TestClass testClass1 = new TestClass();
        TestClass testClass2 = new TestClass();

        HeapDumpSummary dump = HeapDumpSummary.createNew();
        List<HeapDumpSummary.Entry> entries = dump.getEntries();

        HeapDumpSummary.Entry thisClassEntry = entries.stream().filter(entry -> entry.getType().equals(TestClass.class.getName())).findAny().orElse(null);
        assertNotNull(thisClassEntry);
        assertEquals(2, thisClassEntry.getInstances());
        assertEquals(32, thisClassEntry.getBytes());
    }

}
