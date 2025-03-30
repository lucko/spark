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

package me.lucko.spark.sponge;

import com.google.common.collect.ImmutableMap;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import org.spongepowered.api.Game;
import org.spongepowered.plugin.PluginCandidate;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.StandardPluginContainer;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SpongeClassSourceLookup extends ClassSourceLookup.ByCodeSource {
    private final Path modsDirectory;
    private final Map<Path, String> pathToPluginMap;

    public SpongeClassSourceLookup(Game game) {
        this.modsDirectory = game.gameDirectory().resolve("mods").toAbsolutePath().normalize();
        this.pathToPluginMap = constructPathToPluginIdMap(game.pluginManager().plugins());
    }

    @Override
    public String identifyFile(Path path) {
        String id = this.pathToPluginMap.get(path);
        if (id != null) {
            return id;
        }

        if (!path.startsWith(this.modsDirectory)) {
            return null;
        }

        return super.identifyFileName(this.modsDirectory.relativize(path).toString());
    }

    // pretty nasty, but if it fails it doesn't really matter
    private static Map<Path, String> constructPathToPluginIdMap(Collection<PluginContainer> plugins) {
        Map<Path, String> map = new HashMap<>();

        try {
            Field candidateField = StandardPluginContainer.class.getDeclaredField("candidate");
            candidateField.setAccessible(true);

            for (PluginContainer plugin : plugins) {
                if (plugin instanceof StandardPluginContainer) {
                    PluginCandidate candidate = (PluginCandidate) candidateField.get(plugin);
                    Path path = candidate.resource().path().toAbsolutePath().normalize();
                    map.putIfAbsent(path, plugin.metadata().id());
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return ImmutableMap.copyOf(map);
    }

}
