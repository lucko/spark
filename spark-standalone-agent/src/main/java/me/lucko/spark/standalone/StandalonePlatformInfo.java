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

package me.lucko.spark.standalone;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lucko.spark.common.platform.PlatformInfo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class StandalonePlatformInfo implements PlatformInfo {
    private final String version;
    private final String minecraftVersion;

    public StandalonePlatformInfo(String version) {
        this.version = version;
        this.minecraftVersion = detectVanillaMinecraftVersion();
    }

    @Override
    public Type getType() {
        return Type.APPLICATION;
    }

    @Override
    public String getName() {
        return "Standalone";
    }

    @Override
    public String getBrand() {
        return this.minecraftVersion != null ? "Vanilla Minecraft" : "Unknown";
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public String getMinecraftVersion() {
        return this.minecraftVersion;
    }

    private static String detectVanillaMinecraftVersion() {
        try {
            Class<?> clazz = Class.forName("net.minecraft.bundler.Main");
            URL resource = clazz.getClassLoader().getResource("version.json");
            if (resource != null) {
                try (InputStream stream = resource.openStream(); InputStreamReader reader = new InputStreamReader(stream)) {
                    JsonObject obj = new Gson().fromJson(reader, JsonObject.class);
                    JsonElement name = obj.get("name");
                    if (name.isJsonPrimitive() && name.getAsJsonPrimitive().isString()) {
                        return name.getAsString();
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
