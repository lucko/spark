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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Configuration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private JsonObject root;

    public Configuration(Path file) {
        this.file = file;
        load();
    }

    public void load() {
        JsonObject root = null;
        if (Files.exists(this.file)) {
            try (BufferedReader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
                root = GSON.fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (root == null) {
            root = new JsonObject();
            root.addProperty("_header", "spark configuration file - https://spark.lucko.me/docs/Configuration");
        }
        this.root = root;
    }

    public void save() {
        try {
            Files.createDirectories(this.file.getParent());
        } catch (IOException e) {
            // ignore
        }

        try (BufferedWriter writer = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8)) {
            GSON.toJson(this.root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public int getInteger(String path, int def) {
        JsonElement el = this.root.get(path);
        if (el == null || !el.isJsonPrimitive()) {
            return def;
        }

        JsonPrimitive val = el.getAsJsonPrimitive();
        return val.isNumber() ? val.getAsInt() : def;
    }

    public List<String> getStringList(String path) {
        JsonElement el = this.root.get(path);
        if (el == null || !el.isJsonArray()) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<>();
        for (JsonElement child : el.getAsJsonArray()) {
            if (child.isJsonPrimitive()) {
                list.add(child.getAsJsonPrimitive().getAsString());
            }
        }
        return list;
    }

    public void setString(String path, String value) {
        this.root.add(path, new JsonPrimitive(value));
    }

    public void setBoolean(String path, boolean value) {
        this.root.add(path, new JsonPrimitive(value));
    }

    public void setInteger(String path, int value) {
        this.root.add(path, new JsonPrimitive(value));
    }

    public void setStringList(String path, List<String> value) {
        JsonArray array = new JsonArray();
        for (String str : value) {
            array.add(str);
        }
        this.root.add(path, array);
    }

    public boolean contains(String path) {
        return this.root.has(path);
    }

    public void remove(String path) {
        this.root.remove(path);
    }

}
