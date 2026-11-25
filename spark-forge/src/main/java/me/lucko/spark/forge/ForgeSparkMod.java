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
import me.lucko.spark.minecraft.SparkMinecraftMod;
import me.lucko.spark.minecraft.command.CommandWithSuggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

@Mod("spark")
public class ForgeSparkMod implements SparkMinecraftMod {

    private final ModContainer container;
    private final Path configDirectory;

    private ForgeServerSparkPlugin activeServerPlugin;
    private ForgePermissionRegistry permissionRegistry;

    public ForgeSparkMod(FMLJavaModLoadingContext ctx) {
        this.container = ctx.getContainer();
        this.configDirectory = FMLPaths.CONFIGDIR.get().resolve(this.container.getModId());

        FMLClientSetupEvent.getBus(ctx.getModBusGroup()).addListener(this::clientInit);
        ctx.registerDisplayTest(IExtensionPoint.DisplayTest.IGNORE_ALL_VERSION);

        BusGroup.DEFAULT.register(MethodHandles.lookup(), this);
    }

    public void clientInit(FMLClientSetupEvent e) {
        ForgeClientSparkPlugin.init(this, e);
    }

    @SubscribeEvent
    public void serverInit(ServerAboutToStartEvent e) {
        this.activeServerPlugin = ForgeServerSparkPlugin.init(this, e);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent e) {
        if (this.activeServerPlugin != null) {
            this.activeServerPlugin.disable();
            this.activeServerPlugin = null;
        }
    }

    @SubscribeEvent
    public void onServerCommandRegister(RegisterCommandsEvent e) {
        // RegisterCommandsEvent is called before the server is started (ServerAboutToStartEvent),
        // so we register a delegating command here that will delegate to the active server plugin when the server is started.
        CommandWithSuggestions<CommandSourceStack> command = CommandWithSuggestions.forwardingTo(() -> this.activeServerPlugin);
        command.registerToCommandDispatcher(e.getDispatcher(), "spark");
    }

    @SubscribeEvent
    public void onPermissionGather(PermissionGatherEvent.Nodes e) {
        this.permissionRegistry = new ForgePermissionRegistry(e);
    }

    public ForgePermissionRegistry getPermissionRegistry() {
        return this.permissionRegistry;
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
