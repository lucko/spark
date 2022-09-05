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

package me.lucko.spark.api.ping;

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
