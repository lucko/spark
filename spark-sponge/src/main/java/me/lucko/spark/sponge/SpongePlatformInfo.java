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

import me.lucko.spark.common.platform.PlatformInfo;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.plugin.metadata.PluginMetadata;

public class SpongePlatformInfo implements PlatformInfo {
    private final Game game;

    public SpongePlatformInfo(Game game) {
        this.game = game;
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public String getName() {
        return "Sponge";
    }

    @Override
    public String getBrand() {
        PluginMetadata brandMetadata = this.game.platform().container(Platform.Component.IMPLEMENTATION).metadata();
        return brandMetadata.name().orElseGet(brandMetadata::id);
    }

    @Override
    public String getVersion() {
        return this.game.platform().container(Platform.Component.IMPLEMENTATION).metadata().version().toString();
    }

    @Override
    public String getMinecraftVersion() {
        return this.game.platform().minecraftVersion().name();
    }
}
