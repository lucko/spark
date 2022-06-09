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

import me.lucko.spark.proto.SparkSamplerProtos;

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

    public SparkSamplerProtos.ThreadNode toProto(MergeMode mergeMode) {
        SparkSamplerProtos.ThreadNode.Builder proto = SparkSamplerProtos.ThreadNode.newBuilder()
                .setName(getThreadLabel())
                .setTime(getTotalTime());

        for (StackTraceNode child : exportChildren(mergeMode)) {
            proto.addChildren(child.toProto(mergeMode));
        }

        return proto.build();
    }
}
