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

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.nio.file.Path;

@Mod(
        modid = "spark",
        name = "spark",
        version = "@version@",
        acceptableRemoteVersions = "*"
)
public class ForgeSparkMod {

    private Path configDirectory;
    private ForgeServerSparkPlugin activeServerPlugin;

    public String getVersion() {
        return ForgeSparkMod.class.getAnnotation(Mod.class).version();
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        this.configDirectory = e.getModConfigurationDirectory().toPath();
    }

    @EventHandler
    public void clientInit(FMLInitializationEvent e) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            ForgeClientSparkPlugin.register(this);
        }
    }

    @EventHandler
    public void serverInit(FMLServerStartingEvent e) {
        this.activeServerPlugin = ForgeServerSparkPlugin.register(this, e);
    }

    @EventHandler
    public void serverStop(FMLServerStoppingEvent e) {
        if (this.activeServerPlugin != null) {
            this.activeServerPlugin.disable();
            this.activeServerPlugin = null;
        }
    }

    public Path getConfigDirectory() {
        if (this.configDirectory == null) {
            throw new IllegalStateException("Config directory not set");
        }
        return this.configDirectory;
    }
}
