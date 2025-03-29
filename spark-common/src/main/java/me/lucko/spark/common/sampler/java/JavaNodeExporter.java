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

package me.lucko.spark.common.sampler.java;

import me.lucko.spark.common.sampler.node.StackTraceNode;
import me.lucko.spark.common.sampler.node.exporter.AbstractNodeExporter;
import me.lucko.spark.common.sampler.window.ProtoTimeEncoder;
import me.lucko.spark.common.util.MethodDisambiguator;
import me.lucko.spark.proto.SparkSamplerProtos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Node exporter for the {@link JavaSampler}.
 */
public class JavaNodeExporter extends AbstractNodeExporter {
    private final MergeStrategy mergeStrategy;
    private final MethodDisambiguator methodDisambiguator;

    public JavaNodeExporter(ProtoTimeEncoder timeEncoder, MergeStrategy mergeStrategy, MethodDisambiguator methodDisambiguator) {
        super(timeEncoder);
        this.mergeStrategy = mergeStrategy;
        this.methodDisambiguator = methodDisambiguator;
    }

    protected SparkSamplerProtos.StackTraceNode export(StackTraceNode stackTraceNode, Iterable<Integer> childrenRefs) {
        SparkSamplerProtos.StackTraceNode.Builder proto = SparkSamplerProtos.StackTraceNode.newBuilder()
                .setClassName(stackTraceNode.getClassName())
                .setMethodName(stackTraceNode.getMethodName());

        double[] times = stackTraceNode.encodeTimesForProto(this.timeEncoder);
        for (double time : times) {
            proto.addTimes(time);
        }

        int lineNumber = stackTraceNode.getLineNumber();
        if (lineNumber >= 0) {
            proto.setLineNumber(lineNumber);
        }

        if (this.mergeStrategy.separateParentCalls()) {
            int parentLineNumber = stackTraceNode.getParentLineNumber();
            if (parentLineNumber >= 0) {
                proto.setParentLineNumber(parentLineNumber);
            }
        }

        this.methodDisambiguator.disambiguate(stackTraceNode)
                .map(MethodDisambiguator.MethodDescription::getDescription)
                .ifPresent(proto::setMethodDesc);

        proto.addAllChildrenRefs(childrenRefs);

        return proto.build();
    }

    @Override
    protected Collection<StackTraceNode> exportChildren(Collection<StackTraceNode> children) {
        if (children.isEmpty()) {
            return children;
        }

        List<StackTraceNode> list = new ArrayList<>(children.size());

        outer:
        for (StackTraceNode child : children) {
            for (StackTraceNode other : list) {
                if (this.mergeStrategy.shouldMerge(this.methodDisambiguator, other, child)) {
                    other.merge(child);
                    continue outer;
                }
            }
            list.add(child);
        }
        return list;
    }
}
