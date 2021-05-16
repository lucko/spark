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

package me.lucko.spark.api.gc;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Statistics about a garbage collector.
 *
 * <p>All time durations are measured in milliseconds.</p>
 */
public interface GarbageCollector {

    /**
     * Gets the name of the garbage collector.
     *
     * @return the name
     */
    @NonNull String name();

    /**
     * Gets the total number of collections performed.
     *
     * @return the total number of collections
     */
    long totalCollections();

    /**
     * Gets the total amount of time spent performing collections.
     *
     * <p>Measured in milliseconds.</p>
     *
     * @return the total time spent collecting
     */
    long totalTime();

    /**
     * Gets the average amount of time spent performing each collection.
     *
     * <p>Measured in milliseconds.</p>
     *
     * @return the average collection time
     */
    double avgTime();

    /**
     * Gets the average frequency at which collections are performed.
     *
     * <p>Measured in milliseconds.</p>
     *
     * @return the average frequency of collections
     */
    long avgFrequency();

}
