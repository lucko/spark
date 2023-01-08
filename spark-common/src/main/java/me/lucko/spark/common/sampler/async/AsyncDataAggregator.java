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

import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.aggregator.AbstractDataAggregator;
import me.lucko.spark.common.sampler.node.StackTraceNode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;

/**
 * Data aggregator for {@link AsyncSampler}.
 */
public class AsyncDataAggregator extends AbstractDataAggregator {

    /** A describer for async-profiler stack trace elements. */
    private static final StackTraceNode.Describer<AsyncStackTraceElement> STACK_TRACE_DESCRIBER = (element, parent) ->
            new StackTraceNode.Description(element.getClassName(), element.getMethodName(), element.getMethodDescription());

    protected AsyncDataAggregator(ThreadGrouper threadGrouper) {
        super(threadGrouper);
    }

    @Override
    public SamplerMetadata.DataAggregator getMetadata() {
        return SamplerMetadata.DataAggregator.newBuilder()
                .setType(SamplerMetadata.DataAggregator.Type.SIMPLE)
                .setThreadGrouper(this.threadGrouper.asProto())
                .build();
    }

    public void insertData(ProfileSegment element, int window) {
        try {
            ThreadNode node = getNode(this.threadGrouper.getGroup(element.getNativeThreadId(), element.getThreadName()));
            node.log(STACK_TRACE_DESCRIBER, element.getStackTrace(), element.getValue(), window);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
