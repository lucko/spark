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

package me.lucko.spark.common.api;

import me.lucko.spark.api.statistic.Statistic;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Array;
import java.util.Arrays;

public abstract class AbstractStatistic<W extends Enum<W> & StatisticWindow> implements Statistic<W> {
    private final String name;
    protected final W[] windows;

    protected AbstractStatistic(String name, Class<W> enumClass) {
        this.name = name;
        this.windows = enumClass.getEnumConstants();
    }

    @Override
    public @NonNull String name() {
        return this.name;
    }

    @Override
    public W[] getWindows() {
        return Arrays.copyOf(this.windows, this.windows.length);
    }

    public static abstract class Double<W extends Enum<W> & StatisticWindow> extends AbstractStatistic<W> implements DoubleStatistic<W> {
        public Double(String name, Class<W> enumClass) {
            super(name, enumClass);
        }

        @Override
        public double[] poll() {
            double[] values = new double[this.windows.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = poll(this.windows[i]);
            }
            return values;
        }
    }

    public static abstract class Generic<T, W extends Enum<W> & StatisticWindow> extends AbstractStatistic<W> implements GenericStatistic<T, W> {
        private final Class<T> typeClass;

        public Generic(String name, Class<T> typeClass, Class<W> enumClass) {
            super(name, enumClass);
            this.typeClass = typeClass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T[] poll() {
            T[] values = (T[]) Array.newInstance(this.typeClass, this.windows.length);
            for (int i = 0; i < values.length; i++) {
                values[i] = poll(this.windows[i]);
            }
            return values;
        }
    }
}
