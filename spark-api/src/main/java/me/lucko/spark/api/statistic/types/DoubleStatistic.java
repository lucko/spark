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

package me.lucko.spark.api.statistic.types;

import me.lucko.spark.api.statistic.Statistic;
import me.lucko.spark.api.statistic.StatisticWindow;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A {@link Statistic} with a {@code double} value.
 *
 * @param <W> the window type
 */
public interface DoubleStatistic<W extends Enum<W> & StatisticWindow> extends Statistic<W> {

    /**
     * Polls the current value of the statistic in the given window.
     *
     * @param window the window
     * @return the value
     */
    double poll(@NonNull W window);

    /**
     * Polls the current values of the statistic in all windows.
     *
     * @return the values
     */
    double[] poll();

}
