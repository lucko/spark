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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.spark.common.platform.MetadataProvider;

import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;

import java.util.LinkedHashMap;
import java.util.Map;

public class FabricExtraMetadataProvider implements MetadataProvider {

    private final ResourcePackManager resourcePackManager;

    public FabricExtraMetadataProvider(ResourcePackManager resourcePackManager) {
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
        for (ResourcePackProfile profile : this.resourcePackManager.getEnabledProfiles()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", profile.getDisplayName().getString());
            obj.addProperty("description", profile.getDescription().getString());
            obj.addProperty("source", resourcePackSource(profile.getSource()));
            datapacks.add(profile.getName(), obj);
        }
        return datapacks;
    }

    private static String resourcePackSource(ResourcePackSource source) {
        if (source == ResourcePackSource.NONE) {
            return "none";
        } else if (source == ResourcePackSource.BUILTIN) {
            return "builtin";
        } else if (source == ResourcePackSource.WORLD) {
            return "world";
        } else if (source == ResourcePackSource.SERVER) {
            return "server";
        } else {
            return "unknown";
        }
    }
}
