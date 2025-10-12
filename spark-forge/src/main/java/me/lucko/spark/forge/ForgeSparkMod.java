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

package me.lucko.spark.forge;

import me.lucko.spark.forge.plugin.ForgeClientSparkPlugin;
import me.lucko.spark.forge.plugin.ForgeServerSparkPlugin;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

@Mod("spark")
public class ForgeSparkMod {

    private final ModContainer container;
    private final Path configDirectory;

    public ForgeSparkMod(FMLJavaModLoadingContext ctx) {
        this.container = ctx.getContainer();
        this.configDirectory = FMLPaths.CONFIGDIR.get().resolve(this.container.getModId());

        FMLClientSetupEvent.getBus(ctx.getModBusGroup()).addListener(this::clientInit);
        ctx.registerDisplayTest(IExtensionPoint.DisplayTest.IGNORE_ALL_VERSION);

        ServerAboutToStartEvent.BUS.addListener(this::serverInit);
    }

    public String getVersion() {
        return this.container.getModInfo().getVersion().toString();
    }

    public void clientInit(FMLClientSetupEvent e) {
        ForgeClientSparkPlugin.register(this, e);
    }

    public void serverInit(ServerAboutToStartEvent e) {
        ForgeServerSparkPlugin.register(this, e);
    }

    public Path getConfigDirectory() {
        return this.configDirectory;
    }
}
