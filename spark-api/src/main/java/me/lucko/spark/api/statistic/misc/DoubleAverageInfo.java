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

package me.lucko.spark.api.statistic.misc;

/**
 * Statistics for a recorded double value.
 */
public interface DoubleAverageInfo {

    /**
     * Gets the mean value.
     *
     * @return the mean
     */
    double mean();

    /**
     * Gets the max value.
     *
     * @return the max
     */
    double max();

    /**
     * Gets the min value.
     *
     * @return the min
     */
    double min();

    /**
     * Gets the median value.
     *
     * @return the median
     */
    default double median() {
        return percentile(0.50d);
    }

    /**
     * Gets the 95th percentile value.
     *
     * @return the 95th percentile
     */
    default double percentile95th() {
        return percentile(0.95d);
    }

    /**
     * Gets the average value at a given percentile.
     *
     * @param percentile the percentile, as a double between 0 and 1.
     * @return the average value at the given percentile
     */
    double percentile(double percentile);

}
