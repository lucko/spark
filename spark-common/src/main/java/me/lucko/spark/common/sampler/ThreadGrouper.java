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

package me.lucko.spark.common.sampler;

import me.lucko.spark.api.profiler.GrouperChoice;
import me.lucko.spark.common.sampler.grouper.NameThreadGrouper;
import me.lucko.spark.common.sampler.grouper.PoolThreadGrouper;
import me.lucko.spark.common.sampler.grouper.SingleThreadGrouper;
import me.lucko.spark.proto.SparkProtos.SamplerMetadata;

/**
 * Function for grouping threads together
 */
public interface ThreadGrouper {

    ThreadGrouper BY_NAME = new NameThreadGrouper();
    ThreadGrouper BY_POOL = new PoolThreadGrouper();
    ThreadGrouper AS_ONE = new SingleThreadGrouper();

    static ThreadGrouper get(GrouperChoice choice) {
        switch (choice) {
            case SINGLE:
                return AS_ONE;
            case NAME:
                return BY_NAME;
            default:
                return BY_POOL;
        }
    }

    /**
     * Gets the group for the given thread.
     *
     * @param threadId   the id of the thread
     * @param threadName the name of the thread
     * @return the group
     */
    String getGroup(long threadId, String threadName);

    SamplerMetadata.DataAggregator.ThreadGrouper asProto();

}
