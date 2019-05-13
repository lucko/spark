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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Queue;

public class RollingAverage {

    private final Queue<BigDecimal> samples = new ArrayDeque<>();
    private final int size;
    private BigDecimal total = BigDecimal.ZERO;

    public RollingAverage(int size) {
        this.size = size;
    }

    public void add(BigDecimal num) {
        this.total = this.total.add(num);
        this.samples.add(num);
        if (this.samples.size() > this.size) {
            this.total = this.total.subtract(this.samples.remove());
        }
    }

    public double getAverage() {
        if (this.samples.isEmpty()) {
            return 0;
        }
        return this.total.divide(BigDecimal.valueOf(this.samples.size()), 30, RoundingMode.HALF_UP).doubleValue();
    }

}
