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

package me.lucko.spark.common.sampler.window;

import me.lucko.spark.common.sampler.async.jfr.Dictionary;
import me.lucko.spark.common.sampler.node.AbstractNode;
import me.lucko.spark.common.sampler.node.ThreadNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

/**
 * Encodes a map of int->double into a double array.
 */
public class ProtoTimeEncoder {
    /** A sorted array of all possible keys to encode */
    private final int[] keys;
    /** A map of key value -> index in the keys array */
    private final Map<Integer, Integer> keysToIndex;

    public ProtoTimeEncoder(List<ThreadNode> sourceData) {
        // get an array of all keys that show up in the source data
        this.keys = sourceData.stream()
                .map(AbstractNode::getTimeWindows)
                .reduce(IntStream.empty(), IntStream::concat)
                .distinct()
                .sorted()
                .toArray();

        // construct a reverse index lookup
        this.keysToIndex = new HashMap<>(this.keys.length);
        for (int i = 0; i < this.keys.length; i++) {
            this.keysToIndex.put(this.keys[i], i);
        }
    }

    /**
     * Gets an array of the keys that could be encoded by this encoder.
     *
     * @return an array of keys
     */
    public int[] getKeys() {
        return this.keys;
    }

    /**
     * Encode a {@link Dictionary} (map) of times/durations into a double array.
     *
     * @param times a dictionary of times (unix-time millis -> duration in microseconds)
     * @return the times encoded as a double array
     */
    public double[] encode(Dictionary<LongAdder> times) {
        // construct an array of values - length needs to exactly match the
        // number of keys, even if some values are zero.
        double[] array = new double[this.keys.length];

        times.forEach((key, value) -> {
            // get the index for the given key
            Integer idx = this.keysToIndex.get((int) key);
            if (idx == null) {
                throw new RuntimeException("No index for key " + key + " in " + this.keysToIndex.keySet());
            }

            // convert the duration from microseconds -> milliseconds
            double durationInMilliseconds = value.longValue() / 1000d;

            // store in the array
            array[idx] = durationInMilliseconds;
        });

        return array;
    }
}
