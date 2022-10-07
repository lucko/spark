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

import me.lucko.spark.common.sampler.async.jfr.Dictionary;
import me.lucko.spark.common.sampler.window.ProtoTimeEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

/**
 * Encapsulates a timed node in the sampling stack.
 */
public abstract class AbstractNode {

    protected static final int MAX_STACK_DEPTH = 300;

    /** A map of the nodes children */
    private final Map<StackTraceNode.Description, StackTraceNode> children = new ConcurrentHashMap<>();

    /** The accumulated sample time for this node, measured in microseconds */
    // long key = the window (effectively System.currentTimeMillis() / 60_000)
    // LongAdder value = accumulated time in microseconds
    private final Dictionary<LongAdder> times = new Dictionary<>();

    /**
     * Gets the time accumulator for a given window
     *
     * @param window the window
     * @return the accumulator
     */
    protected LongAdder getTimeAccumulator(int window) {
        LongAdder adder = this.times.get(window);
        if (adder == null) {
            adder = new LongAdder();
            this.times.put(window, adder);
        }
        return adder;
    }

    /**
     * Gets the time windows that have been logged for this node.
     *
     * @return the time windows
     */
    public IntStream getTimeWindows() {
        IntStream.Builder keys = IntStream.builder();
        this.times.forEach((key, value) -> keys.add((int) key));
        return keys.build();
    }

    /**
     * Gets the encoded total sample times logged for this node in milliseconds.
     *
     * @return the total times
     */
    protected double[] encodeTimesForProto(ProtoTimeEncoder encoder) {
        return encoder.encode(this.times);
    }

    public Collection<StackTraceNode> getChildren() {
        return this.children.values();
    }

    protected StackTraceNode resolveChild(StackTraceNode.Description description) {
        StackTraceNode result = this.children.get(description); // fast path
        if (result != null) {
            return result;
        }
        return this.children.computeIfAbsent(description, StackTraceNode::new);
    }

    /**
     * Merge {@code other} into {@code this}.
     *
     * @param other the other node
     */
    protected void merge(AbstractNode other) {
        other.times.forEach((key, value) -> getTimeAccumulator((int) key).add(value.longValue()));
        for (Map.Entry<StackTraceNode.Description, StackTraceNode> child : other.children.entrySet()) {
            resolveChild(child.getKey()).merge(child.getValue());
        }
    }

    protected List<StackTraceNode> exportChildren(MergeMode mergeMode) {
        if (this.children.isEmpty()) {
            return Collections.emptyList();
        }

        List<StackTraceNode> list = new ArrayList<>(this.children.size());

        outer:
        for (StackTraceNode child : this.children.values()) {
            // attempt to find an existing node we can merge into
            for (StackTraceNode other : list) {
                if (mergeMode.shouldMerge(other, child)) {
                    other.merge(child);
                    continue outer;
                }
            }

            // just add
            list.add(child);
        }

        //list.sort(null);
        return list;
    }

}
