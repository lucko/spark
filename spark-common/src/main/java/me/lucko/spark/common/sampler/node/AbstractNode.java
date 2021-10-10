/*
 * This file is part of spark.
 *
 *  Copyright (C) Albert Pham <http://www.sk89q.com>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Encapsulates a timed node in the sampling stack.
 */
public abstract class AbstractNode {

    private static final int MAX_STACK_DEPTH = 300;

    /** A map of the nodes children */
    private final Map<StackTraceNode.Description, StackTraceNode> children = new ConcurrentHashMap<>();

    /** The accumulated sample time for this node, measured in microseconds */
    private final LongAdder totalTime = new LongAdder();

    /**
     * Gets the total sample time logged for this node in milliseconds.
     *
     * @return the total time
     */
    public double getTotalTime() {
        return this.totalTime.longValue() / 1000d;
    }

    public Collection<StackTraceNode> getChildren() {
        return this.children.values();
    }

    /**
     * Logs the given stack trace against this node and its children.
     *
     * @param describer the function that describes the elements of the stack
     * @param stack the stack
     * @param time the total time to log
     * @param <T> the stack trace element type
     */
    public <T> void log(StackTraceNode.Describer<T> describer, T[] stack, long time) {
        if (stack.length == 0) {
            return;
        }

        this.totalTime.add(time);

        AbstractNode node = this;
        T previousElement = null;

        for (int offset = 0; offset < Math.min(MAX_STACK_DEPTH, stack.length); offset++) {
            T element = stack[(stack.length - 1) - offset];

            node = node.resolveChild(describer.describe(element, previousElement));
            node.totalTime.add(time);

            previousElement = element;
        }
    }

    private StackTraceNode resolveChild(StackTraceNode.Description description) {
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
        this.totalTime.add(other.totalTime.longValue());
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

        list.sort(null);
        return list;
    }

}
