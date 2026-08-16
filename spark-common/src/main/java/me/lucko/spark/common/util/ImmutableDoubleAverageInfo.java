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

package me.lucko.spark.common.util;

import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;

public class ImmutableDoubleAverageInfo implements DoubleAverageInfo {
    private final double mean;
    private final double max;
    private final double min;
    private final double median;
    private final double percentile95th;

    public ImmutableDoubleAverageInfo(double mean, double max, double min, double median, double percentile95th) {
        this.mean = mean;
        this.max = max;
        this.min = min;
        this.median = median;
        this.percentile95th = percentile95th;
    }
    
    public ImmutableDoubleAverageInfo(DoubleAverageInfo other) {
        this.mean = other.mean();
        this.max = other.max();
        this.min = other.min();
        this.median = other.median();
        this.percentile95th = other.percentile95th();
    }

    @Override
    public double mean() {
        return this.mean;
    }

    @Override
    public double max() {
        return this.max;
    }

    @Override
    public double min() {
        return this.min;
    }

    @Override
    public double median() {
        return this.median;
    }

    @Override
    public double percentile95th() {
        return this.percentile95th;
    }

    @Override
    public double percentile(double percentile) {
        throw new UnsupportedOperationException();
    }
}
