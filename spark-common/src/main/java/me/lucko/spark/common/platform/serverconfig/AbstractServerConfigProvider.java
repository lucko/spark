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

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract implementation of {@link ServerConfigProvider}.
 *
 * <p>This implementation is able to delete hidden paths from
 * the configurations before they are sent to the viewer.</p>
 */
public abstract class AbstractServerConfigProvider implements ServerConfigProvider {
    private final Map<String, ConfigParser> files;
    private final Collection<String> hiddenPaths;

    private final Gson gson;

    protected AbstractServerConfigProvider(Map<String, ConfigParser> files, Collection<String> hiddenPaths) {
        this.files = files;
        this.hiddenPaths = hiddenPaths;

        GsonBuilder gson = new GsonBuilder();
        customiseGson(gson);
        this.gson = gson.create();
    }

    @Override
    public final Map<String, JsonElement> loadServerConfigurations() {
        ImmutableMap.Builder<String, JsonElement> builder = ImmutableMap.builder();

        this.files.forEach((path, reader) -> {
            try {
                JsonElement json = load(path, reader);
                if (json == null) {
                    return;
                }

                delete(json, this.hiddenPaths);
                builder.put(path, json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return builder.build();
    }

    private JsonElement load(String path, ConfigParser parser) throws IOException {
        Map<String, Object> values = parser.parse(path);
        if (values == null) {
            return null;
        }

        return this.gson.toJsonTree(values);
    }

    protected void customiseGson(GsonBuilder gson) {

    }

    /**
     * Deletes the given paths from the json element.
     *
     * @param json the json element
     * @param paths the paths to delete
     */
    private static void delete(JsonElement json, Collection<String> paths) {
        for (String path : paths) {
            Deque<String> pathDeque = new LinkedList<>(Arrays.asList(path.split("\\.")));
            delete(json, pathDeque);
        }
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
