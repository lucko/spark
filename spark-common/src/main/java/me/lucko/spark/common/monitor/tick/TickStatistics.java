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

package me.lucko.spark.common.monitor.tick;

import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;

/**
 * Provides the server TPS (ticks per second) and MSPT (milliseconds per tick) rates.
 */
public interface TickStatistics {

    double tps5Sec();
    double tps10Sec();
    double tps1Min();
    double tps5Min();
    double tps15Min();

    boolean isDurationSupported();

    DoubleAverageInfo duration10Sec();
    DoubleAverageInfo duration1Min();
    DoubleAverageInfo duration5Min();

}
