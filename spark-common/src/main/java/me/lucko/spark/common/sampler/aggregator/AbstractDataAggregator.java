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

package me.lucko.spark.common.sampler.aggregator;

import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.node.ThreadNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;

/**
 * Abstract implementation of {@link DataAggregator}.
 */
public abstract class AbstractDataAggregator implements DataAggregator {

    /** A map of root stack nodes for each thread with sampling data */
    protected final Map<String, ThreadNode> threadData = new ConcurrentHashMap<>();

    /** The instance used to group threads together */
    protected final ThreadGrouper threadGrouper;

    protected AbstractDataAggregator(ThreadGrouper threadGrouper) {
        this.threadGrouper = threadGrouper;
    }

    protected ThreadNode getNode(String group) {
        ThreadNode node = this.threadData.get(group); // fast path
        if (node != null) {
            return node;
        }
        return this.threadData.computeIfAbsent(group, ThreadNode::new);
    }

    @Override
    public void pruneData(IntPredicate timeWindowPredicate) {
        this.threadData.values().removeIf(node -> node.removeTimeWindowsRecursively(timeWindowPredicate));
    }

    @Override
    public List<ThreadNode> exportData() {
        List<ThreadNode> data = new ArrayList<>(this.threadData.values());
        for (ThreadNode node : data) {
            node.setThreadLabel(this.threadGrouper.getLabel(node.getThreadGroup()));
        }
        return data;
    }
}
