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

package me.lucko.spark.common.sampler.node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import me.lucko.spark.common.sampler.SamplerMode;
import me.lucko.spark.common.sampler.async.AsyncNodeExporter;
import me.lucko.spark.common.sampler.async.AsyncStackTraceElement;
import me.lucko.spark.common.sampler.window.ProtoTimeEncoder;
import me.lucko.spark.proto.SparkSamplerProtos;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeTest {

    private static final StackTraceNode.Describer<AsyncStackTraceElement> STACK_TRACE_DESCRIBER = (element, parent) -> new StackTraceNode.AsyncDescription(element.getClassName(), element.getMethodName(), element.getMethodDescription());
    private static final int WINDOW = 10;

    private static final AsyncStackTraceElement NODE_0 = new AsyncStackTraceElement("java.lang.Thread", "run", "()V");
    private static final AsyncStackTraceElement NODE_1_1 = new AsyncStackTraceElement("test.Foo", "run", "()V");
    private static final AsyncStackTraceElement NODE_1_2_1 = new AsyncStackTraceElement("test.Foo", "example", "()V");
    private static final AsyncStackTraceElement NODE_2_1 = new AsyncStackTraceElement("test.Bar", "run", "()V");
    private static final AsyncStackTraceElement NODE_2_2_1 = new AsyncStackTraceElement("test.Bar", "example", "()V");

    private static final AsyncStackTraceElement[] STACK_1 = {NODE_1_2_1, NODE_1_1, NODE_0};
    private static final AsyncStackTraceElement[] STACK_2 = {NODE_2_2_1, NODE_2_1, NODE_0};

    @Test
    public void testThreadLabels() {
        ThreadNode node = new ThreadNode("Test Thread");
        assertEquals("Test Thread", node.getThreadGroup());
        assertEquals("Test Thread", node.getThreadLabel());

        node.setThreadLabel("Test");
        assertEquals("Test", node.getThreadLabel());
    }

    @Test
    public void testBasicLog() {
        ThreadNode threadNode = new ThreadNode("Test Thread");
        assertEquals(0, threadNode.getTimeWindows().size());

        threadNode.log(STACK_TRACE_DESCRIBER, STACK_1, TimeUnit.SECONDS.toMicros(1), WINDOW);

        Collection<StackTraceNode> children1 = threadNode.getChildren();
        assertEquals(1, children1.size());
        assertEquals(ImmutableSet.of(WINDOW), threadNode.getTimeWindows());

        StackTraceNode node1 = children1.iterator().next();
        assertEquals(ImmutableSet.of(WINDOW), node1.getTimeWindows());
        assertEquals("java.lang.Thread", node1.getClassName());
        assertEquals("run", node1.getMethodName());
        assertEquals("()V", node1.getMethodDescription());
        assertEquals(StackTraceNode.NULL_LINE_NUMBER, node1.getLineNumber());
        assertEquals(StackTraceNode.NULL_LINE_NUMBER, node1.getParentLineNumber());
        assertEquals(TimeUnit.SECONDS.toMicros(1), node1.getTimeAccumulator(WINDOW).longValue());

        threadNode.log(STACK_TRACE_DESCRIBER, STACK_2, TimeUnit.SECONDS.toMicros(1), WINDOW);
        assertEquals(TimeUnit.SECONDS.toMicros(2), node1.getTimeAccumulator(WINDOW).longValue());

        Collection<StackTraceNode> children2 = node1.getChildren();
        assertEquals(2, children2.size());

        for (StackTraceNode node2 : children2) {
            assertEquals(ImmutableSet.of(WINDOW), node2.getTimeWindows());
            assertEquals(TimeUnit.SECONDS.toMicros(1), node2.getTimeAccumulator(WINDOW).longValue());
        }
    }

    @Test
    public void testToProto() {
        ThreadNode threadNode = new ThreadNode("Test Thread");
        threadNode.log(STACK_TRACE_DESCRIBER, STACK_1, TimeUnit.SECONDS.toMicros(1), WINDOW);
        threadNode.log(STACK_TRACE_DESCRIBER, STACK_1, TimeUnit.SECONDS.toMicros(1), WINDOW + 1);
        threadNode.log(STACK_TRACE_DESCRIBER, STACK_2, TimeUnit.SECONDS.toMicros(1), WINDOW + 1);

        ProtoTimeEncoder timeEncoder = new ProtoTimeEncoder(SamplerMode.EXECUTION.valueTransformer(), ImmutableList.of(threadNode));
        int[] keys = timeEncoder.getKeys();
        assertArrayEquals(new int[]{WINDOW, WINDOW + 1}, keys);

        SparkSamplerProtos.ThreadNode proto = new AsyncNodeExporter(timeEncoder).export(threadNode);

        SparkSamplerProtos.ThreadNode expected = SparkSamplerProtos.ThreadNode.newBuilder()
                .setName("Test Thread")
                .addTimes(1000)
                .addTimes(2000)
                .addChildren(SparkSamplerProtos.StackTraceNode.newBuilder()
                        .setClassName("test.Bar")
                        .setMethodDesc("()V")
                        .setMethodName("example")
                        .addTimes(0)
                        .addTimes(1000)
                )
                .addChildren(SparkSamplerProtos.StackTraceNode.newBuilder()
                        .setClassName("test.Bar")
                        .setMethodDesc("()V")
                        .setMethodName("run")
                        .addTimes(0)
                        .addTimes(1000)
                        .addChildrenRefs(0)
                )
                .addChildren(SparkSamplerProtos.StackTraceNode.newBuilder()
                        .setClassName("test.Foo")
                        .setMethodDesc("()V")
                        .setMethodName("example")
                        .addTimes(1000)
                        .addTimes(1000)
                )
                .addChildren(SparkSamplerProtos.StackTraceNode.newBuilder()
                        .setClassName("test.Foo")
                        .setMethodDesc("()V")
                        .setMethodName("run")
                        .addTimes(1000)
                        .addTimes(1000)
                        .addChildrenRefs(2)
                )
                .addChildren(SparkSamplerProtos.StackTraceNode.newBuilder()
                        .setClassName("java.lang.Thread")
                        .setMethodDesc("()V")
                        .setMethodName("run")
                        .addTimes(1000)
                        .addTimes(2000)
                        .addChildrenRefs(1)
                        .addChildrenRefs(3)
                )
                .addChildrenRefs(4)
                .build();

        assertEquals(expected, proto);
    }

    @Test
    public void testRemoveTimeWindows() {
        ThreadNode threadNode = new ThreadNode("Test Thread");
        threadNode.log(STACK_TRACE_DESCRIBER, STACK_1, TimeUnit.SECONDS.toMicros(1), WINDOW);
        threadNode.log(STACK_TRACE_DESCRIBER, STACK_2, TimeUnit.SECONDS.toMicros(1), WINDOW + 1);

        StackTraceNode threadRunNode = threadNode.getChildren().iterator().next();
        Collection<StackTraceNode> fooBarNodes = threadRunNode.getChildren();

        assertEquals(2, threadNode.getTimeWindows().size());
        assertEquals(2, threadRunNode.getChildren().size());
        assertEquals(2, threadRunNode.getTimeWindows().size());

        for (StackTraceNode node : fooBarNodes) {
            assertEquals(1, node.getTimeWindows().size());
            assertEquals(1, node.getChildren().size());
            assertEquals(1, node.getChildren().iterator().next().getTimeWindows().size());
            assertEquals(0, node.getChildren().iterator().next().getChildren().size());
        }

        assertFalse(threadNode.removeTimeWindowsRecursively(w -> w == WINDOW));
        assertEquals(1, threadNode.getTimeWindows().size());
        assertEquals(1, threadRunNode.getChildren().size());
        assertEquals(1, threadRunNode.getTimeWindows().size());

        assertTrue(threadNode.removeTimeWindowsRecursively(w -> w == WINDOW + 1));
        assertEquals(0, threadNode.getTimeWindows().size());
        assertEquals(0, threadNode.getChildren().size());

        // doesn't bother updating nested children that have been removed
        for (StackTraceNode node : fooBarNodes) {
            assertEquals(1, node.getTimeWindows().size());
            assertEquals(1, node.getChildren().size());
            assertEquals(1, node.getChildren().iterator().next().getTimeWindows().size());
            assertEquals(0, node.getChildren().iterator().next().getChildren().size());
        }
    }

}
