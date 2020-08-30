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

import me.lucko.spark.fabric.plugin.FabricClientSparkPlugin;
import me.lucko.spark.fabric.plugin.FabricServerSparkPlugin;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;
import java.util.Objects;

public class FabricSparkMod implements ModInitializer {
    private static FabricSparkMod mod;

    private ModContainer container;
    private Path configDirectory;

    @Override
    public void onInitialize() {
        FabricSparkMod.mod = this;

        FabricLoader loader = FabricLoader.getInstance();
        this.container = loader.getModContainer("spark")
                .orElseThrow(() -> new IllegalStateException("Unable to get container for spark"));
        this.configDirectory = loader.getConfigDir().resolve("spark");

        // load hooks
        ServerLifecycleEvents.SERVER_STARTING.register(server -> FabricServerSparkPlugin.register(this, server));
    }

    // called be entrypoint defined in fabric.mod.json
    public static void initializeClient() {
        Objects.requireNonNull(FabricSparkMod.mod, "mod");
        FabricClientSparkPlugin.register(FabricSparkMod.mod, MinecraftClient.getInstance());
    }

    public String getVersion() {
        return this.container.getMetadata().getVersion().getFriendlyString();
    }

    public Path getConfigDirectory() {
        if (this.configDirectory == null) {
            throw new IllegalStateException("Config directory not set");
        }
        return this.configDirectory;
    }
}
