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

package me.lucko.spark.common.platform;

import com.google.gson.JsonElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Function to export dynamic metadata to be displayed within the spark viewer.
 */
@FunctionalInterface
public interface MetadataProvider {

    /**
     * Produces a map of the metadata.
     *
     * @return the metadata
     */
    Map<String, JsonElement> get();

    default Map<String, String> export() {
        Map<String, String> map = new LinkedHashMap<>();
        get().forEach((key, value) -> map.put(key, value.toString()));
        return map;
    }

}
