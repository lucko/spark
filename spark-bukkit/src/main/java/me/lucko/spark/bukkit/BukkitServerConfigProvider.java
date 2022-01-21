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

package me.lucko.spark.bukkit;

import co.aikar.timings.TimingsManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;

import me.lucko.spark.common.platform.serverconfig.AbstractServerConfigProvider;

import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class BukkitServerConfigProvider extends AbstractServerConfigProvider<BukkitServerConfigProvider.FileType> {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(MemorySection.class, (JsonSerializer<MemorySection>) (obj, type, ctx) -> ctx.serialize(obj.getValues(false)))
            .create();

    private static final Map<String, FileType> FILES = ImmutableMap.<String, FileType>builder()
            .put("server.properties", FileType.PROPERTIES)
            .put("bukkit.yml", FileType.YAML)
            .put("spigot.yml", FileType.YAML)
            .put("paper.yml", FileType.YAML)
            .put("pufferfish.yml", FileType.YAML)
            .put("purpur.yml", FileType.YAML)
            .build();

    // todo: make configurable?
    private static final List<String> HIDDEN_PATHS = ImmutableList.<String>builder()
            .addAll(TimingsManager.hiddenConfigs)
            .add("database")
            .add("settings.bungeecord-addresses")
            .add("settings.velocity-support.secret")
            .build();

    public BukkitServerConfigProvider() {
        super(FILES, HIDDEN_PATHS);
    }

    @Override
    protected JsonElement load(String path, FileType type) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            return null;
        }

        switch (type) {
            case PROPERTIES: {
                try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    Properties properties = new Properties();
                    properties.load(reader);

                    Map<String, String> values = new HashMap<>();
                    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                        values.put(entry.getKey().toString(), entry.getValue().toString());
                    }

                    return GSON.toJsonTree(values);
                }
            }
            case YAML: {
                try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
                    Map<String, Object> values = config.getValues(false);
                    return GSON.toJsonTree(values);
                }
            }
            default: {
                throw new IllegalArgumentException("Unknown file type: " + type);
            }
        }
    }

    enum FileType {
        PROPERTIES,
        YAML
    }

}
