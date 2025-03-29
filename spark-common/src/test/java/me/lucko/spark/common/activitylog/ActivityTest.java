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
import com.google.gson.JsonElement;
import me.lucko.spark.common.command.sender.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActivityTest {
    private static final Gson GSON = new Gson();

    private static final CommandSender.Data USER = new CommandSender.Data("Test", UUID.fromString("5937921d-7051-45e1-bac7-3bbfdc12444f"));

    private static final String FILE_ACTIVITY_JSON = "{\"user\":{\"type\":\"player\",\"name\":\"Test\",\"uniqueId\":\"5937921d-7051-45e1-bac7-3bbfdc12444f\"},\"time\":1721937782184,\"type\":\"Profiler\",\"data\":{\"type\":\"file\",\"value\":\"path/to/profile.sparkprofile\"}}";
    private static final String URL_ACTIVITY_JSON = "{\"user\":{\"type\":\"player\",\"name\":\"Test\",\"uniqueId\":\"5937921d-7051-45e1-bac7-3bbfdc12444f\"},\"time\":1721937782184,\"type\":\"Profiler\",\"data\":{\"type\":\"url\",\"value\":\"https://spark.lucko.me/abcd\"}}";

    @Test
    public void testSerialize() {
        Activity fileActivity = Activity.fileActivity(
                USER,
                1721937782184L,
                "Profiler",
                "path/to/profile.sparkprofile"
        );
        assertEquals(FILE_ACTIVITY_JSON, GSON.toJson(fileActivity.serialize()));

        Activity urlActivity = Activity.urlActivity(
                USER,
                1721937782184L,
                "Profiler",
                "https://spark.lucko.me/abcd"
        );
        assertEquals(URL_ACTIVITY_JSON, GSON.toJson(urlActivity.serialize()));
    }

    @Test
    public void testDeserialize() {
        Activity fileActivity = Activity.deserialize(GSON.fromJson(FILE_ACTIVITY_JSON, JsonElement.class));
        assertEquals(USER.getUniqueId(), fileActivity.getUser().getUniqueId());
        assertEquals(USER.getName(), fileActivity.getUser().getName());
        assertEquals(1721937782184L, fileActivity.getTime());
        assertEquals("Profiler", fileActivity.getType());
        assertEquals(Activity.DATA_TYPE_FILE, fileActivity.getDataType());
        assertEquals("path/to/profile.sparkprofile", fileActivity.getDataValue());

        Activity urlActivity = Activity.deserialize(GSON.fromJson(URL_ACTIVITY_JSON, JsonElement.class));
        assertEquals(USER.getUniqueId(), urlActivity.getUser().getUniqueId());
        assertEquals(USER.getName(), urlActivity.getUser().getName());
        assertEquals(1721937782184L, urlActivity.getTime());
        assertEquals("Profiler", urlActivity.getType());
        assertEquals(Activity.DATA_TYPE_URL, urlActivity.getDataType());
        assertEquals("https://spark.lucko.me/abcd", urlActivity.getDataValue());
    }

}
