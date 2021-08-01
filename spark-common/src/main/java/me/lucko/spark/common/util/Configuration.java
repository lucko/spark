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

package me.lucko.spark.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Configuration {
    private static final JsonParser PARSER = new JsonParser();

    private final JsonObject root;

    public Configuration(Path file) {
        JsonObject root = null;
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                root = PARSER.parse(reader).getAsJsonObject();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.root = root != null ? root : new JsonObject();
    }

    public String getString(String path, String def) {
        JsonElement el = this.root.get(path);
        if (el == null || !el.isJsonPrimitive()) {
            return def;
        }

        return el.getAsJsonPrimitive().getAsString();
    }

    public boolean getBoolean(String path, boolean def) {
        JsonElement el = this.root.get(path);
        if (el == null || !el.isJsonPrimitive()) {
            return def;
        }

        JsonPrimitive val = el.getAsJsonPrimitive();
        return val.isBoolean() ? val.getAsBoolean() : def;
    }

}
