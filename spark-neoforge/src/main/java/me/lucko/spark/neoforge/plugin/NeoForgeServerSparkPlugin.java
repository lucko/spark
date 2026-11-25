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

package me.lucko.spark.neoforge.plugin;

import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.minecraft.plugin.MinecraftServerSparkPlugin;
import me.lucko.spark.minecraft.sender.MinecraftServerCommandSender;
import me.lucko.spark.neoforge.NeoForgeClassSourceLookup;
import me.lucko.spark.neoforge.NeoForgePermissionRegistry;
import me.lucko.spark.neoforge.NeoForgePlatformInfo;
import me.lucko.spark.neoforge.NeoForgeServerCommandSender;
import me.lucko.spark.neoforge.NeoForgeSparkMod;
import me.lucko.spark.neoforge.NeoForgeTickHook;
import me.lucko.spark.neoforge.NeoForgeTickReporter;
import me.lucko.spark.neoforge.NeoForgeWorldInfoProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.Collection;

public class NeoForgeServerSparkPlugin extends MinecraftServerSparkPlugin<NeoForgeSparkMod> {

    public static NeoForgeServerSparkPlugin init(NeoForgeSparkMod mod, ServerAboutToStartEvent event) {
        NeoForgeServerSparkPlugin plugin = new NeoForgeServerSparkPlugin(mod, event.getServer());
        plugin.enable();
        return plugin;
    }

    public NeoForgeServerSparkPlugin(NeoForgeSparkMod mod, MinecraftServer server) {
        super(mod, server);
    }

    public PermissionNode<Boolean> getPermissionNode(String permission) {
        NeoForgePermissionRegistry permissionRegistry = this.mod.getPermissionRegistry();
        if (permissionRegistry == null) {
            throw new IllegalStateException("Permission registry not initialized");
        }
        return permissionRegistry.getPermissionNode(permission);
    }

    @Override
    protected MinecraftServerCommandSender createCommandSender(CommandSourceStack source) {
        return new NeoForgeServerCommandSender(source, this);
    }

    @Override
    public TickHook createTickHook() {
        return new NeoForgeTickHook.Server();
    }

    @Override
    public TickReporter createTickReporter() {
        return new NeoForgeTickReporter.Server();
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new NeoForgeClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                ModList.get().getMods(),
                IModInfo::getModId,
                mod -> mod.getVersion().toString(),
                mod -> null, // ?
                IModInfo::getDescription
        );
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new NeoForgeWorldInfoProvider.Server(this.server);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new NeoForgePlatformInfo(PlatformInfo.Type.SERVER);
    }
}
