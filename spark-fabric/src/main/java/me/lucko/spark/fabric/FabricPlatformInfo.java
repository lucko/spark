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

import me.lucko.spark.common.platform.PlatformInfo;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

public class FabricPlatformInfo implements PlatformInfo {
    private final Type type;

    public FabricPlatformInfo(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String getName() {
        return "Fabric";
    }

    @Override
    public String getVersion() {
        return getModVersion("fabricloader").orElse("unknown");
    }

    @Override
    public String getMinecraftVersion() {
        return getModVersion("minecraft").orElse(null);
    }

    private Optional<String> getModVersion(String mod) {
        return FabricLoader.getInstance().getModContainer(mod).map(container -> container.getMetadata().getVersion().getFriendlyString());
    }
}
