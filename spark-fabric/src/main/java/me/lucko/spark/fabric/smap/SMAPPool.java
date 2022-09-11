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

package me.lucko.spark.fabric.smap;

import java.util.concurrent.ConcurrentHashMap;

public class SMAPPool {

    private static final ConcurrentHashMap<String, String> SOURCE_DEBUG_POOL = new ConcurrentHashMap<>();

    public static void add(String className, String sourceDebug) {
        if (className == null || sourceDebug == null) return;
        SOURCE_DEBUG_POOL.put(className, sourceDebug);
    }

    public static String getPooled(String className) {
        return SOURCE_DEBUG_POOL.get(className);
    }

    private SMAPPool() {
    }

}
