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

import me.lucko.spark.common.platform.PlatformInfo;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;

public class NeoForgePlatformInfo implements PlatformInfo {
    private final Type type;

    public NeoForgePlatformInfo(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String getName() {
        return "NeoForge";
    }

    @Override
    public String getBrand() {
        return ModList.get().getModContainerById("neoforge")
                .map(ModContainer::getModInfo)
                .map(IModInfo::getDisplayName)
                .orElse("NeoForge");
    }

    @Override
    public String getVersion() {
        return FMLLoader.getCurrent().getVersionInfo().neoForgeVersion();
    }

    @Override
    public String getMinecraftVersion() {
        return FMLLoader.getCurrent().getVersionInfo().mcVersion();
    }
}
