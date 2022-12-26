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
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;

import java.util.List;
import java.util.function.IntPredicate;

/**
 * Aggregates sampling data.
 */
public interface DataAggregator {

    /**
     * Forms the output data
     *
     * @return the output data
     */
    List<ThreadNode> exportData();

    /**
     * Prunes windows of data from this aggregator if the given {@code timeWindowPredicate} returns true.
     *
     * @param timeWindowPredicate the predicate
     */
    void pruneData(IntPredicate timeWindowPredicate);

    /**
     * Gets metadata about the data aggregator instance.
     */
    SamplerMetadata.DataAggregator getMetadata();

}
