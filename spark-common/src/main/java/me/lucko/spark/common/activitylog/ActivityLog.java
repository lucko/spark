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

package me.lucko.spark.common.activitylog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class ActivityLog {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonParser PARSER = new JsonParser();

    private final Path file;

    private final LinkedList<Activity> log = new LinkedList<>();
    private final Object[] mutex = new Object[0];

    public ActivityLog(Path file) {
        this.file = file;
    }

    public void addToLog(Activity activity) {
        synchronized (this.mutex) {
            this.log.addFirst(activity);
        }
        save();
    }

    public List<Activity> getLog() {
        synchronized (this.mutex) {
            return new LinkedList<>(this.log);
        }
    }

    public void save() {
        JsonArray array = new JsonArray();
        synchronized (this.mutex) {
            for (Activity activity : this.log) {
                if (!activity.shouldExpire()) {
                    array.add(activity.serialize());
                }
            }
        }

        try {
            Files.createDirectories(this.file.getParent());
        } catch (IOException e) {
            // ignore
        }

        try (BufferedWriter writer = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8)) {
            GSON.toJson(array, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!Files.exists(this.file)) {
            synchronized (this.mutex) {
                this.log.clear();
                return;
            }
        }

        JsonArray array;
        try (BufferedReader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
            array = PARSER.parse(reader).getAsJsonArray();
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            return;
        }

        boolean save = false;

        synchronized (this.mutex) {
            this.log.clear();
            for (JsonElement element : array) {
                try {
                    Activity activity = Activity.deserialize(element);
                    if (activity.shouldExpire()) {
                        save = true;
                    } else {
                        this.log.add(activity);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (save) {
            try {
                save();
            } catch (Exception e) {
                // ignore
            }
        }
    }

}
