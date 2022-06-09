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

package me.lucko.spark.common.monitor.ping;

import java.util.Arrays;

public final class PingSummary {

    private final int[] values;
    private final int total;
    private final int max;
    private final int min;
    private final double mean;

    public PingSummary(int[] values) {
        Arrays.sort(values);
        this.values = values;

        int total = 0;
        for (int value : values) {
            total += value;
        }
        this.total = total;

        this.mean = (double) total / values.length;
        this.max = values[values.length - 1];
        this.min = values[0];
    }

    public int total() {
        return this.total;
    }

    public double mean() {
        return this.mean;
    }

    public int max() {
        return this.max;
    }

    public int min() {
        return this.min;
    }

    public int percentile(double percentile) {
        if (percentile < 0 || percentile > 1) {
            throw new IllegalArgumentException("Invalid percentile " + percentile);
        }

        int rank = (int) Math.ceil(percentile * (this.values.length - 1));
        return this.values[rank];
    }

    public double median() {
        return percentile(0.50d);
    }

    public double percentile95th() {
        return percentile(0.95d);
    }

}
