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

package me.lucko.spark.api;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Singleton provider for {@link Spark}.
 */
public final class SparkProvider {

    private static Spark instance;

    /**
     * Gets the singleton spark API instance.
     *
     * @return the spark API instance
     */
    public static @NonNull Spark get() {
        Spark instance = SparkProvider.instance;
        if (instance == null) {
            throw new IllegalStateException("spark has not loaded yet!");
        }
        return instance;
    }

    static void set(Spark impl) {
        SparkProvider.instance = impl;
    }

    private SparkProvider() {
        throw new AssertionError();
    }

}
