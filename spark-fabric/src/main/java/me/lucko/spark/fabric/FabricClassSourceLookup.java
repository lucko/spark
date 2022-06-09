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

package me.lucko.spark.fabric;

import com.google.common.collect.ImmutableMap;

import me.lucko.spark.common.util.ClassSourceLookup;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public class FabricClassSourceLookup extends ClassSourceLookup.ByCodeSource {
    private final Path modsDirectory;
    private final Map<Path, String> pathToModMap;

    public FabricClassSourceLookup() {
        FabricLoader loader = FabricLoader.getInstance();
        this.modsDirectory = loader.getGameDir().resolve("mods").toAbsolutePath().normalize();
        this.pathToModMap = constructPathToModIdMap(loader.getAllMods());
    }

    @Override
    public String identifyFile(Path path) {
        String id = this.pathToModMap.get(path);
        if (id != null) {
            return id;
        }

        if (!path.startsWith(this.modsDirectory)) {
            return null;
        }

        return super.identifyFileName(this.modsDirectory.relativize(path).toString());
    }

    private static Map<Path, String> constructPathToModIdMap(Collection<ModContainer> mods) {
        ImmutableMap.Builder<Path, String> builder = ImmutableMap.builder();
        for (ModContainer mod : mods) {
            Path path = mod.getRootPath().toAbsolutePath().normalize();
            builder.put(path, mod.getMetadata().getId());
        }
        return builder.build();
    }
}
