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
