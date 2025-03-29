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

package me.lucko.spark.common.sampler.async;

import me.lucko.spark.common.sampler.node.StackTraceNode;
import me.lucko.spark.common.sampler.node.exporter.AbstractNodeExporter;
import me.lucko.spark.common.sampler.window.ProtoTimeEncoder;
import me.lucko.spark.proto.SparkSamplerProtos;

import java.util.Collection;

/**
 * Node exporter for the {@link AsyncSampler}.
 */
public class AsyncNodeExporter extends AbstractNodeExporter {
    public AsyncNodeExporter(ProtoTimeEncoder timeEncoder) {
        super(timeEncoder);
    }

    @Override
    protected SparkSamplerProtos.StackTraceNode export(StackTraceNode stackTraceNode, Iterable<Integer> childrenRefs) {
        SparkSamplerProtos.StackTraceNode.Builder proto = SparkSamplerProtos.StackTraceNode.newBuilder()
                .setClassName(stackTraceNode.getClassName())
                .setMethodName(stackTraceNode.getMethodName());

        double[] times = stackTraceNode.encodeTimesForProto(this.timeEncoder);
        for (double time : times) {
            proto.addTimes(time);
        }

        String methodDescription = stackTraceNode.getMethodDescription();
        if (methodDescription != null) {
            proto.setMethodDesc(methodDescription);
        }

        proto.addAllChildrenRefs(childrenRefs);

        return proto.build();
    }

    @Override
    protected Collection<StackTraceNode> exportChildren(Collection<StackTraceNode> children) {
        return children;
    }
}
