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
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;

import me.lucko.spark.common.platform.serverconfig.AbstractServerConfigProvider;
import me.lucko.spark.common.platform.serverconfig.ConfigParser;
import me.lucko.spark.common.platform.serverconfig.PropertiesConfigParser;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import co.aikar.timings.TimingsManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BukkitServerConfigProvider extends AbstractServerConfigProvider {

    /** A map of provided files and their type */
    private static final Map<String, ConfigParser> FILES;
    /** A collection of paths to be excluded from the files */
    private static final Collection<String> HIDDEN_PATHS;

    public BukkitServerConfigProvider() {
        super(FILES, HIDDEN_PATHS);
    }

    @Override
    protected void customiseGson(GsonBuilder gson) {
       gson.registerTypeAdapter(MemorySection.class, (JsonSerializer<MemorySection>) (obj, type, ctx) -> ctx.serialize(obj.getValues(false)));
    }

    private enum YamlConfigParser implements ConfigParser {
        INSTANCE;

        @Override
        public Map<String, Object> parse(BufferedReader reader) throws IOException {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            return config.getValues(false);
        }
    }

    private enum SplitYamlConfigParser implements ConfigParser {
        INSTANCE;

        @Override
        public Map<String, Object> parse(String file) throws IOException {
            String group = file.replace("/", "");
            Path configDir = Paths.get("config");
            if (!Files.exists(configDir)) {
                return null;
            }

            Map<String, Object> configs = Maps.newHashMap();
            configs.put("global.yml", parse(Files.newBufferedReader(configDir.resolve(group + "-global.yml"))));
            configs.put("world-defaults.yml", parse(Files.newBufferedReader(configDir.resolve(group + "-world-defaults.yml"))));
            for (World world : Bukkit.getWorlds()) {
                configs.put("world-" + world.getName() + ".yml", parse(Files.newBufferedReader(world.getWorldFolder().toPath().resolve(group + "-world.yml"))));
            }

            return configs;
        }

        @Override
        public Map<String, Object> parse(BufferedReader reader) throws IOException {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            return config.getValues(false);
        }
    }

    static {
        ImmutableMap.Builder<String, ConfigParser> files = ImmutableMap.<String, ConfigParser>builder()
                .put("server.properties", PropertiesConfigParser.INSTANCE)
                .put("bukkit.yml", YamlConfigParser.INSTANCE)
                .put("spigot.yml", YamlConfigParser.INSTANCE)
                .put("paper.yml", YamlConfigParser.INSTANCE)
                .put("paper/", SplitYamlConfigParser.INSTANCE)
                .put("purpur.yml", YamlConfigParser.INSTANCE);

        for (String config : getSystemPropertyList("spark.serverconfigs.extra")) {
            files.put(config, YamlConfigParser.INSTANCE);
        }

        ImmutableSet.Builder<String> hiddenPaths = ImmutableSet.<String>builder()
                .add("database")
                .add("settings.bungeecord-addresses")
                .add("settings.velocity-support.secret")
                .add("proxies.velocity.secret")
                .add("server-ip")
                .add("motd")
                .add("resource-pack")
                .add("rcon<dot>password")
                .add("level-seed")
                .add("world-settings.*.feature-seeds")
                .add("world-settings.*.seed-*")
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
