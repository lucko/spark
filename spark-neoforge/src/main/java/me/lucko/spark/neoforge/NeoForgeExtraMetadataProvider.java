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

package me.lucko.spark.neoforge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lucko.spark.common.platform.MetadataProvider;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;

import java.util.LinkedHashMap;
import java.util.Map;

public class NeoForgeExtraMetadataProvider implements MetadataProvider {

    private final PackRepository resourcePackManager;

    public NeoForgeExtraMetadataProvider(PackRepository resourcePackManager) {
        this.resourcePackManager = resourcePackManager;
    }

    @Override
    public Map<String, JsonElement> get() {
        Map<String, JsonElement> metadata = new LinkedHashMap<>();
        metadata.put("datapacks", datapackMetadata());
        return metadata;
    }

    private JsonElement datapackMetadata() {
        JsonObject datapacks = new JsonObject();
        for (Pack profile : this.resourcePackManager.getSelectedPacks()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", profile.getTitle().getString());
            obj.addProperty("description", profile.getDescription().getString());
            obj.addProperty("source", resourcePackSource(profile.getPackSource()));
            datapacks.add(profile.getId(), obj);
        }
        return datapacks;
    }

    private static String resourcePackSource(PackSource source) {
        if (source == PackSource.DEFAULT) {
            return "none";
        } else if (source == PackSource.BUILT_IN) {
            return "builtin";
        } else if (source == PackSource.WORLD) {
            return "world";
        } else if (source == PackSource.SERVER) {
            return "server";
        } else {
            return "unknown";
        }
    }
}
