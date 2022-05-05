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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;

import me.lucko.spark.common.platform.serverconfig.AbstractServerConfigProvider;
import me.lucko.spark.common.platform.serverconfig.PropertiesFileReader;

import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import co.aikar.timings.TimingsManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BukkitServerConfigProvider extends AbstractServerConfigProvider<BukkitServerConfigProvider.FileType> {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(MemorySection.class, (JsonSerializer<MemorySection>) (obj, type, ctx) -> ctx.serialize(obj.getValues(false)))
            .create();

    /** A map of provided files and their type */
    private static final Map<String, FileType> FILES;
    /** A collection of paths to be excluded from the files */
    private static final Collection<String> HIDDEN_PATHS;

    public BukkitServerConfigProvider() {
        super(FILES, HIDDEN_PATHS);
    }

    @Override
    protected JsonElement load(String path, FileType type) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            Map<String, Object> values;

            if (type == FileType.PROPERTIES) {
                PropertiesFileReader propertiesReader = new PropertiesFileReader(reader);
                values = propertiesReader.readProperties();
            } else if (type == FileType.YAML) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
                values = config.getValues(false);
            } else {
                throw new IllegalArgumentException("Unknown file type: " + type);
            }

            return GSON.toJsonTree(values);
        }
    }

    enum FileType {
        PROPERTIES,
        YAML
    }

    static {
        ImmutableMap.Builder<String, FileType> files = ImmutableMap.<String, FileType>builder()
                .put("server.properties", FileType.PROPERTIES)
                .put("bukkit.yml", FileType.YAML)
                .put("spigot.yml", FileType.YAML)
                .put("paper.yml", FileType.YAML)
                .put("purpur.yml", FileType.YAML);

        for (String config : getSystemPropertyList("spark.serverconfigs.extra")) {
            files.put(config, FileType.YAML);
        }

        ImmutableSet.Builder<String> hiddenPaths = ImmutableSet.<String>builder()
                .add("database")
                .add("settings.bungeecord-addresses")
                .add("settings.velocity-support.secret")
                .add("server-ip")
                .add("motd")
                .add("resource-pack")
                .add("rcon<dot>password")
                .add("level-seed")
                .add("world-settings.*.feature-seeds")
                .addAll(getTimingsHiddenConfigs())
                .addAll(getSystemPropertyList("spark.serverconfigs.hiddenpaths"));

        FILES = files.build();
        HIDDEN_PATHS = hiddenPaths.build();
    }

    private static List<String> getSystemPropertyList(String property) {
        String value = System.getProperty(property);
        return value == null
                ? Collections.emptyList()
                : Arrays.asList(value.split(","));
    }

    private static List<String> getTimingsHiddenConfigs() {
        try {
            return TimingsManager.hiddenConfigs;
        } catch (NoClassDefFoundError e) {
            return Collections.emptyList();
        }
    }

}
