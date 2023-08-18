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
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

import me.lucko.spark.common.platform.serverconfig.ConfigParser;
import me.lucko.spark.common.platform.serverconfig.ExcludedConfigFilter;
import me.lucko.spark.common.platform.serverconfig.PropertiesConfigParser;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;

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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BukkitServerConfigProvider extends ServerConfigProvider {

    /** A map of provided files and their type */
    private static final Map<String, ConfigParser> FILES;
    /** A collection of paths to be excluded from the files */
    private static final Collection<String> HIDDEN_PATHS;

    public BukkitServerConfigProvider() {
        super(FILES, HIDDEN_PATHS);
    }

    private static class YamlConfigParser implements ConfigParser {
        public static final YamlConfigParser INSTANCE = new YamlConfigParser();
        protected static final Gson GSON = new GsonBuilder()
                .registerTypeAdapter(MemorySection.class, (JsonSerializer<MemorySection>) (obj, type, ctx) -> ctx.serialize(obj.getValues(false)))
                .create();

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
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            return config.getValues(false);
        }
    }

    // Paper 1.19+ split config layout
    private static class SplitYamlConfigParser extends YamlConfigParser {
        public static final SplitYamlConfigParser INSTANCE = new SplitYamlConfigParser();

        @Override
        public JsonElement load(String group, ExcludedConfigFilter filter) throws IOException {
            String prefix = group.replace("/", "");

            Path configDir = Paths.get("config");
            if (!Files.exists(configDir)) {
                return null;
            }

            JsonObject root = new JsonObject();

            for (Map.Entry<String, Path> entry : getNestedFiles(configDir, prefix).entrySet()) {
                String fileName = entry.getKey();
                Path path = entry.getValue();

                Map<String, Object> values = this.parse(path);
                if (values == null) {
                    continue;
                }

                // apply the filter individually to each nested file
                root.add(fileName, filter.apply(GSON.toJsonTree(values)));
            }

            return root;
        }

        private static Map<String, Path> getNestedFiles(Path configDir, String prefix) {
            Map<String, Path> files = new LinkedHashMap<>();
            files.put("global.yml", configDir.resolve(prefix + "-global.yml"));
            files.put("world-defaults.yml", configDir.resolve(prefix + "-world-defaults.yml"));
            for (World world : Bukkit.getWorlds()) {
                files.put(world.getName() + ".yml", world.getWorldFolder().toPath().resolve(prefix + "-world.yml"));
            }
            return files;
        }
    }

    static {
        ImmutableMap.Builder<String, ConfigParser> files = ImmutableMap.<String, ConfigParser>builder()
                .put("server.properties", PropertiesConfigParser.INSTANCE)
                .put("bukkit.yml", YamlConfigParser.INSTANCE)
                .put("spigot.yml", YamlConfigParser.INSTANCE)
                .put("paper.yml", YamlConfigParser.INSTANCE)
                .put("paper/", SplitYamlConfigParser.INSTANCE)
                .put("purpur.yml", YamlConfigParser.INSTANCE)
                .put("pufferfish.yml", YamlConfigParser.INSTANCE);

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
                .add("feature-seeds")
                .add("seed-*")
                .addAll(getTimingsHiddenConfigs())
                .addAll(getSystemPropertyList("spark.serverconfigs.hiddenpaths"));

        FILES = files.build();
        HIDDEN_PATHS = hiddenPaths.build();
    }

    private static List<String> getTimingsHiddenConfigs() {
        try {
            return TimingsManager.hiddenConfigs;
        } catch (NoClassDefFoundError e) {
            return Collections.emptyList();
        }
    }

}
