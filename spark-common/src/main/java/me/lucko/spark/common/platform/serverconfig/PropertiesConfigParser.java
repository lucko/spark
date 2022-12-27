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

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A {@link ConfigParser} that can parse a .properties file.
 */
public enum PropertiesConfigParser implements ConfigParser {
    INSTANCE;

    private static final Gson GSON = new Gson();

    @Override
    public JsonElement load(String file, ExcludedConfigFilter filter) throws IOException {
        Map<String, Object> values = this.parse(Paths.get(file));
        if (values == null) {
            return null;
        }

        return filter.apply(GSON.toJsonTree(values));
    }

    @Override
    public Map<String, Object> parse(BufferedReader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);

        Map<String, Object> values = new HashMap<>();
        properties.forEach((k, v) -> {
            String key = k.toString();
            String value = v.toString();

            if ("true".equals(value) || "false".equals(value)) {
                values.put(key, Boolean.parseBoolean(value));
            } else if (value.matches("\\d+")) {
                try {
                    values.put(key, Long.parseLong(value));
                } catch (NumberFormatException e) {
                    values.put(key, value);
                }
            } else {
                values.put(key, value);
            }
        });

        return values;
    }

}
