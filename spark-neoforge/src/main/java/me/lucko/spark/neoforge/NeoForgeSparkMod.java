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

import me.lucko.spark.minecraft.SparkMinecraftMod;
import me.lucko.spark.neoforge.plugin.NeoForgeClientSparkPlugin;
import me.lucko.spark.neoforge.plugin.NeoForgeServerSparkPlugin;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.nio.file.Path;

@Mod("spark")
public class NeoForgeSparkMod implements SparkMinecraftMod {

    private final ModContainer container;
    private final Path configDirectory;

    public NeoForgeSparkMod(ModContainer container, IEventBus eventBus) {
        this.container = container;
        this.configDirectory = FMLPaths.CONFIGDIR.get().resolve(this.container.getModId());

        eventBus.addListener(this::clientInit);

        NeoForge.EVENT_BUS.register(this);
    }

    public void clientInit(FMLClientSetupEvent e) {
        NeoForgeClientSparkPlugin.init(this, e);
    }

    @SubscribeEvent
    public void serverInit(ServerAboutToStartEvent e) {
        NeoForgeServerSparkPlugin.init(this, e);
    }

    @Override
    public String getVersion() {
        return this.container.getModInfo().getVersion().toString();
    }

    @Override
    public Path getConfigDirectory() {
        return this.configDirectory;
    }
}
