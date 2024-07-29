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

import me.lucko.spark.proto.SparkHeapProtos;
import me.lucko.spark.test.TestClass;
import me.lucko.spark.test.plugin.TestCommandSender;
import me.lucko.spark.test.plugin.TestSparkPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HeapDumpSummaryTest {

    @Test
    public void testHeapDumpSummary(@TempDir Path directory) throws Exception {
        TestClass testClass1 = new TestClass();
        TestClass testClass2 = new TestClass();

        HeapDumpSummary dump = HeapDumpSummary.createNew();
        List<HeapDumpSummary.Entry> entries = dump.getEntries();

        HeapDumpSummary.Entry thisClassEntry = entries.stream().filter(entry -> entry.getType().equals(TestClass.class.getName())).findAny().orElse(null);
        assertNotNull(thisClassEntry);
        assertEquals(2, thisClassEntry.getInstances());
        assertEquals(32, thisClassEntry.getBytes());

        SparkHeapProtos.HeapData proto;
        try (TestSparkPlugin plugin = new TestSparkPlugin(directory)) {
            proto = dump.toProto(plugin.platform(), TestCommandSender.INSTANCE.toData());
        }
        assertNotNull(proto);

        SparkHeapProtos.HeapEntry protoEntry = proto.getEntriesList().stream().filter(entry -> entry.getType().equals(TestClass.class.getName())).findAny().orElse(null);
        assertNotNull(protoEntry);
        assertEquals(2, protoEntry.getInstances());
        assertEquals(32, protoEntry.getSize());
    }

}
