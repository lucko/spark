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

package me.lucko.spark.common.sampler.node.exporter;

import me.lucko.spark.common.sampler.node.StackTraceNode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.common.sampler.window.ProtoTimeEncoder;
import me.lucko.spark.common.util.IndexedListBuilder;
import me.lucko.spark.proto.SparkSamplerProtos;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractNodeExporter implements NodeExporter {
    protected final ProtoTimeEncoder timeEncoder;

    protected AbstractNodeExporter(ProtoTimeEncoder timeEncoder) {
        this.timeEncoder = timeEncoder;
    }

    @Override
    public SparkSamplerProtos.ThreadNode export(ThreadNode threadNode) {
        SparkSamplerProtos.ThreadNode.Builder proto = SparkSamplerProtos.ThreadNode.newBuilder()
                .setName(threadNode.getThreadLabel());

        double[] times = threadNode.encodeTimesForProto(this.timeEncoder);
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
        for (StackTraceNode child : exportChildren(threadNode.getChildren())) {
            stack.push(new Node(child, childrenRefs));
        }

        Node node;
        while (!stack.isEmpty()) {
            node = stack.peek();

            // on the first visit, just push this node's children and leave it on the stack
            if (node.firstVisit) {
                for (StackTraceNode child : exportChildren(node.stackTraceNode.getChildren())) {
                    stack.push(new Node(child, node.childrenRefs));
                }
                node.firstVisit = false;
                continue;
            }

            // convert StackTraceNode to a proto
            // - at this stage, we have already visited this node's children
            // - the refs for each child are stored in node.childrenRefs
            SparkSamplerProtos.StackTraceNode childProto = this.export(node.stackTraceNode, node.childrenRefs);

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

    protected abstract SparkSamplerProtos.StackTraceNode export(StackTraceNode stackTraceNode, Iterable<Integer> childrenRefs);

    protected abstract Collection<StackTraceNode> exportChildren(Collection<StackTraceNode> children);

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
