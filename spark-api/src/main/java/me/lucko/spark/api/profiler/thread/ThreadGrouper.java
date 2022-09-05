/*
 * This file is part of spark, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.spark.api.profiler.thread;

import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata.DataAggregator;

/**
 * Function for grouping threads together
 */
public interface ThreadGrouper {

    /**
     * Implementation of {@link ThreadGrouper} that just groups by thread name.
     */
    ThreadGrouper BY_NAME = SparkProvider.get().grouper(DataAggregator.ThreadGrouper.BY_NAME);

    /**
     * Implementation of {@link ThreadGrouper} that attempts to group by the name of the pool
     * the thread originated from.
     *
     * <p>The regex pattern used to match pools expects a digit at the end of the thread name,
     * separated from the pool name with any of one or more of ' ', '-', or '#'.</p>
     */
    ThreadGrouper BY_POOL = SparkProvider.get().grouper(DataAggregator.ThreadGrouper.BY_POOL);

    /**
     * Implementation of {@link ThreadGrouper} which groups all threads as one, under
     * the name "All".
     */
    ThreadGrouper AS_ONE = SparkProvider.get().grouper(DataAggregator.ThreadGrouper.AS_ONE);

    /**
     * Gets the group for the given thread.
     *
     * @param threadId   the id of the thread
     * @param threadName the name of the thread
     * @return the group
     */
    String getGroup(long threadId, String threadName);

    /**
     * Gets the label to use for a given group.
     *
     * @param group the group
     * @return the label
     */
    String getLabel(String group);

    /**
     * Gets the proto equivalent of this grouper. <br>
     * If this is a custom grouper, use {@link DataAggregator.ThreadGrouper#UNRECOGNIZED}
     *
     * @return the proto equivalent
     */
    DataAggregator.ThreadGrouper asProto();
}
