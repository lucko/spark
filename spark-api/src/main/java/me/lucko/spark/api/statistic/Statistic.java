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

package me.lucko.spark.api.statistic;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A statistic for which a rolling average in various windows is recorded.
 *
 * @param <W> the window type
 */
public interface Statistic<W extends Enum<W> & StatisticWindow> {

    /**
     * Gets the statistic name.
     *
     * @return the statistic name
     */
    @NonNull String name();

    /**
     * Gets the windows used for rolling averages.
     *
     * @return the windows
     */
    @NonNull W[] getWindows();

}
