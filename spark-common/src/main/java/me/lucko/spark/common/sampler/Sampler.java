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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.common.util.ClassSourceLookup;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract superinterface for all sampler implementations.
 */
public interface Sampler {

    /**
     * Starts the sampler.
     */
    void start();

    /**
     * Stops the sampler.
     */
    void stop();

    /**
     * Gets the time when the sampler started (unix timestamp in millis)
     *
     * @return the start time
     */
    long getStartTime();

    /**
     * Gets the time when the sampler should automatically stop (unix timestamp in millis)
     *
     * @return the end time, or -1 if undefined
     */
    long getEndTime();

    /**
     * Gets a future to encapsulate the completion of the sampler
     *
     * @return a future
     */
    CompletableFuture<Sampler> getFuture();

    // Methods used to export the sampler data to the web viewer.
    SamplerData toProto(SparkPlatform platform, CommandSender creator, Comparator<? super Map.Entry<String, ThreadNode>> outputOrder, String comment, MergeMode mergeMode, ClassSourceLookup classSourceLookup);

}
