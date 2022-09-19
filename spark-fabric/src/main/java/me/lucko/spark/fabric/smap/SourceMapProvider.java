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

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SourceMapProvider {
    private final Map<String, SourceMap> cache = new HashMap<>();

    public @Nullable SourceMap getSourceMap(String className) {
        if (this.cache.containsKey(className)) {
            return this.cache.get(className);
        }

        SourceMap smap = null;
        try {
            String value = SourceDebugCache.getSourceDebugInfo(className);
            if (value != null) {
                value = value.replaceAll("\r\n?", "\n");
                if (value.startsWith("SMAP\n")) {
                    smap = new SourceMap(value);
                }
            }
        } catch (Exception e) {
            // ignore
        }

        this.cache.put(className, smap);
        return smap;
    }

}
