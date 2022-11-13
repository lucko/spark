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

import me.lucko.spark.common.sampler.window.ProtoTimeEncoder;
import me.lucko.spark.common.util.IndexedListBuilder;
import me.lucko.spark.proto.SparkSamplerProtos;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.IntPredicate;

/**
 * The root of a sampling stack for a given thread / thread group.
 */
public final class ThreadNode extends AbstractNode {

    /**
     * The name of this thread / thread group
     */
    private final String name;

    /**
     * The label used to describe this thread in the viewer
     */
    public String label;

    public ThreadNode(String name) {
        this.name = name;
    }

    public String getThreadLabel() {
        return this.label != null ? this.label : this.name;
    }

    public String getThreadGroup() {
        return this.name;
    }

    public void setThreadLabel(String label) {
        this.label = label;
    }

    /**
     * Logs the given stack trace against this node and its children.
     *
     * @param describer the function that describes the elements of the stack
     * @param stack the stack
     * @param time the total time to log
     * @param window the window
     * @param <T> the stack trace element type
     */
    public <T> void log(StackTraceNode.Describer<T> describer, T[] stack, long time, int window) {
        if (stack.length == 0) {
            return;
        }

        getTimeAccumulator(window).add(time);

        AbstractNode node = this;
        T previousElement = null;

        for (int offset = 0; offset < Math.min(MAX_STACK_DEPTH, stack.length); offset++) {
            T element = stack[(stack.length - 1) - offset];

            node = node.resolveChild(describer.describe(element, previousElement));
            node.getTimeAccumulator(window).add(time);

            previousElement = element;
        }
    }

    /**
     * Removes time windows that match the given {@code predicate}.
     *
     * @param predicate the predicate to use to test the time windows
     * @return true if this node is now empty
     */
    public boolean removeTimeWindowsRecursively(IntPredicate predicate) {
        Queue<AbstractNode> queue = new ArrayDeque<>();
        queue.add(this);

        while (!queue.isEmpty()) {
            AbstractNode node = queue.remove();
            Collection<StackTraceNode> children = node.getChildren();

            boolean needToProcessChildren = false;

            for (Iterator<StackTraceNode> it = children.iterator(); it.hasNext(); ) {
                StackTraceNode child = it.next();

                boolean windowsWereRemoved = child.removeTimeWindows(predicate);
                boolean childIsNowEmpty = child.getTimeWindows().isEmpty();

                if (childIsNowEmpty) {
                    it.remove();
                    continue;
                }

                if (windowsWereRemoved) {
                    needToProcessChildren = true;
                }
            }

            if (needToProcessChildren) {
                queue.addAll(children);
            }
        }

        removeTimeWindows(predicate);
        return getTimeWindows().isEmpty();
    }

    public SparkSamplerProtos.ThreadNode toProto(MergeMode mergeMode, ProtoTimeEncoder timeEncoder) {
        SparkSamplerProtos.ThreadNode.Builder proto = SparkSamplerProtos.ThreadNode.newBuilder()
                .setName(getThreadLabel());

        double[] times = encodeTimesForProto(timeEncoder);
        for (double time : times) {
            proto.addTimes(time);
        }

        // When converting to a proto, we change the data structure from a recursive tree to an array.
        // Effectively, instead of:
        //
        //   {
        //     data: 'one',
        //     children: [
        //       {
        //         data: 'two',
        //         children: [{ data: 'four' }]
        //       },
        //       { data: 'three' }
        //     ]
        //   }
        //
        // we transmit:
        //
        //   [
        //     { data: 'one', children: [1, 2] },
        //     { data: 'two', children: [3] }
        //     { data: 'three', children: [] }
        //     { data: 'four', children: [] }
        //   ]
        //

        // the flattened array of nodes
        IndexedListBuilder<SparkSamplerProtos.StackTraceNode> nodesArray = new IndexedListBuilder<>();

        // Perform a depth-first post order traversal of the tree
        Deque<Node> stack = new ArrayDeque<>();

        // push the thread node's children to the stack
        List<Integer> childrenRefs = new LinkedList<>();
        for (StackTraceNode child : exportChildren(mergeMode)) {
            stack.push(new Node(child, childrenRefs));
        }

        Node node;
        while (!stack.isEmpty()) {
            node = stack.peek();

            // on the first visit, just push this node's children and leave it on the stack
            if (node.firstVisit) {
                for (StackTraceNode child : node.stackTraceNode.exportChildren(mergeMode)) {
                    stack.push(new Node(child, node.childrenRefs));
                }
                node.firstVisit = false;
                continue;
            }

            // convert StackTraceNode to a proto
            // - at this stage, we have already visited this node's children
            // - the refs for each child are stored in node.childrenRefs
            SparkSamplerProtos.StackTraceNode childProto = node.stackTraceNode.toProto(mergeMode, timeEncoder, node.childrenRefs);

            // add the child proto to the nodes array, and record the ref in the parent
            int childIndex = nodesArray.add(childProto);
            node.parentChildrenRefs.add(childIndex);

            // pop from the stack
            stack.pop();
        }

        proto.addAllChildrenRefs(childrenRefs);
        proto.addAllChildren(nodesArray.build());

        return proto.build();
    }

    private static final class Node {
        private final StackTraceNode stackTraceNode;
        private boolean firstVisit = true;
        private final List<Integer> childrenRefs = new LinkedList<>();
        private final List<Integer> parentChildrenRefs;

        private Node(StackTraceNode node, List<Integer> parentChildrenRefs) {
            this.stackTraceNode = node;
            this.parentChildrenRefs = parentChildrenRefs;
        }
    }
}
