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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import me.lucko.spark.common.command.sender.CommandSender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        } catch (IOException e) {
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
                    }
                    this.log.add(activity);
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

    public static final class Activity {
        private final CommandSender.Data user;
        private final long time;
        private final String type;

        private final String dataType;
        private final String dataValue;

        public static Activity urlActivity(CommandSender user, long time, String type, String url) {
            return new Activity(user.toData(), time, type, "url", url);
        }

        public static Activity fileActivity(CommandSender user, long time, String type, String filePath) {
            return new Activity(user.toData(), time, type, "file", filePath);
        }

        private Activity(CommandSender.Data user, long time, String type, String dataType, String dataValue) {
            this.user = user;
            this.time = time;
            this.type = type;
            this.dataType = dataType;
            this.dataValue = dataValue;
        }

        public CommandSender.Data getUser() {
            return this.user;
        }

        public long getTime() {
            return this.time;
        }

        public String getType() {
            return this.type;
        }

        public String getDataType() {
            return this.dataType;
        }

        public String getDataValue() {
            return this.dataValue;
        }

        public boolean shouldExpire() {
            if (this.dataType.equals("url")) {
                return (System.currentTimeMillis() - this.time) > TimeUnit.DAYS.toMillis(7);
            } else {
                return false;
            }
        }

        public JsonObject serialize() {
            JsonObject object = new JsonObject();

            object.add("user", this.user.serialize());
            object.add("time", new JsonPrimitive(this.time));
            object.add("type", new JsonPrimitive(this.type));

            JsonObject data = new JsonObject();
            data.add("type", new JsonPrimitive(this.dataType));
            data.add("value", new JsonPrimitive(this.dataValue));
            object.add("data", data);

            return object;
        }

        public static Activity deserialize(JsonElement element) {
            JsonObject object = element.getAsJsonObject();

            CommandSender.Data user = CommandSender.Data.deserialize(object.get("user"));
            long time = object.get("time").getAsJsonPrimitive().getAsLong();
            String type = object.get("type").getAsJsonPrimitive().getAsString();

            JsonObject dataObject = object.get("data").getAsJsonObject();
            String dataType = dataObject.get("type").getAsJsonPrimitive().getAsString();
            String dataValue = dataObject.get("value").getAsJsonPrimitive().getAsString();

            return new Activity(user, time, type, dataType, dataValue);
        }
    }

}
