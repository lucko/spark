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

package me.lucko.spark.forge.plugin;

import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.forge.ForgeClassSourceLookup;
import me.lucko.spark.forge.ForgePermissionRegistry;
import me.lucko.spark.forge.ForgePlatformInfo;
import me.lucko.spark.forge.ForgeServerCommandSender;
import me.lucko.spark.forge.ForgeSparkMod;
import me.lucko.spark.forge.ForgeTickHook;
import me.lucko.spark.forge.ForgeTickReporter;
import me.lucko.spark.forge.ForgeWorldInfoProvider;
import me.lucko.spark.minecraft.plugin.MinecraftServerSparkPlugin;
import me.lucko.spark.minecraft.sender.MinecraftServerCommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.server.permission.nodes.PermissionNode;

import java.util.Collection;

public class ForgeServerSparkPlugin extends MinecraftServerSparkPlugin<ForgeSparkMod> {

    public static ForgeServerSparkPlugin init(ForgeSparkMod mod, ServerAboutToStartEvent event) {
        ForgeServerSparkPlugin plugin = new ForgeServerSparkPlugin(mod, event.getServer());
        plugin.enable();
        return plugin;
    }

    public ForgeServerSparkPlugin(ForgeSparkMod mod, MinecraftServer server) {
        super(mod, server);
    }

    public PermissionNode<Boolean> getPermissionNode(String permission) {
        ForgePermissionRegistry permissionRegistry = this.mod.getPermissionRegistry();
        if (permissionRegistry == null) {
            throw new IllegalStateException("Permission registry not initialized");
        }
        return permissionRegistry.getPermissionNode(permission);
    }

    @Override
    protected MinecraftServerCommandSender createCommandSender(CommandSourceStack source) {
        return new ForgeServerCommandSender(source, this);
    }

    @Override
    public TickHook createTickHook() {
        return new ForgeTickHook.Server();
    }

    @Override
    public TickReporter createTickReporter() {
        return new ForgeTickReporter.Server();
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new ForgeClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                ModList.getMods(),
                IModInfo::getModId,
                mod -> mod.getVersion().toString(),
                mod -> null, // ?
                IModInfo::getDescription
        );
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new ForgeWorldInfoProvider.Server(this.server);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new ForgePlatformInfo(PlatformInfo.Type.SERVER);
    }
}
