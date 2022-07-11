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

package me.lucko.spark.common.platform.serverconfig;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filtered excluded paths from {@link JsonElement}s (parsed configuration files).
 */
public class ExcludedConfigFilter {
    private final Collection<String> pathsToExclude;

    public ExcludedConfigFilter(Collection<String> pathsToExclude) {
        this.pathsToExclude = pathsToExclude;
    }

    /**
     * Deletes the excluded paths from the json element.
     *
     * @param json the json element
     */
    public JsonElement apply(JsonElement json) {
        for (String path : this.pathsToExclude) {
            Deque<String> pathDeque = new LinkedList<>(Arrays.asList(path.split("\\.")));
            delete(json, pathDeque);
        }
        return json;
    }

    private static void delete(JsonElement json, Deque<String> path) {
        if (path.isEmpty()) {
            return;
        }
        if (!json.isJsonObject()) {
            return;
        }

        JsonObject jsonObject = json.getAsJsonObject();
        String expected = path.removeFirst().replace("<dot>", ".");

        Collection<String> keys;
        if (expected.equals("*")) {
            keys = jsonObject.entrySet().stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } else if (expected.endsWith("*")) {
            String pattern = expected.substring(0, expected.length() - 1);
            keys = jsonObject.entrySet().stream()
                    .map(Map.Entry::getKey)
                    .filter(key -> key.startsWith(pattern))
                    .collect(Collectors.toList());
        } else if (jsonObject.has(expected)) {
            keys = Collections.singletonList(expected);
        } else {
            keys = Collections.emptyList();
        }

        for (String key : keys) {
            if (path.isEmpty()) {
                jsonObject.remove(key);
            } else {
                Deque<String> pathCopy = keys.size() > 1
                        ? new LinkedList<>(path)
                        : path;

                delete(jsonObject.get(key), pathCopy);
            }
        }
    }
}
