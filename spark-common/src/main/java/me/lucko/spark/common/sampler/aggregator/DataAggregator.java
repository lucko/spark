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

import me.lucko.spark.common.sampler.node.ThreadNode;

import java.util.Map;

/**
 * Aggregates sampling data.
 */
public interface DataAggregator {

    /**
     * Forms the output data
     *
     * @return the output data
     */
    Map<String, ThreadNode> getData();

    /**
     * Inserts sampling data into this aggregator
     * @param threadId the id of the thread
     * @param threadName the name of the thread
     * @param stack the call stack
     */
    void insertData(long threadId, String threadName, StackTraceElement[] stack);

}
