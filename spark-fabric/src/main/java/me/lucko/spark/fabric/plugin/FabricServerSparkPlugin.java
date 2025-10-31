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

package me.lucko.spark.fabric.plugin;

import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.fabric.FabricClassSourceLookup;
import me.lucko.spark.fabric.FabricPlatformInfo;
import me.lucko.spark.fabric.FabricServerCommandSender;
import me.lucko.spark.fabric.FabricSparkMod;
import me.lucko.spark.fabric.FabricTickHook;
import me.lucko.spark.fabric.FabricTickReporter;
import me.lucko.spark.fabric.FabricWorldInfoProvider;
import me.lucko.spark.fabric.placeholder.SparkFabricPlaceholderApi;
import me.lucko.spark.minecraft.plugin.MinecraftServerSparkPlugin;
import me.lucko.spark.minecraft.sender.MinecraftServerCommandSender;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.util.Collection;
import java.util.stream.Collectors;

public class FabricServerSparkPlugin extends MinecraftServerSparkPlugin<FabricSparkMod> {

    public static FabricServerSparkPlugin init(FabricSparkMod mod, MinecraftServer server) {
        FabricServerSparkPlugin plugin = new FabricServerSparkPlugin(mod, server);
        plugin.enable();
        return plugin;
    }

    public FabricServerSparkPlugin(FabricSparkMod mod, MinecraftServer server) {
        super(mod, server);
    }

    @Override
    public void enable() {
        super.enable();

        // placeholders
        if (FabricLoader.getInstance().isModLoaded("placeholder-api")) {
            try {
                SparkFabricPlaceholderApi.register(this.platform);
            } catch (LinkageError e) {
                // ignore
            }
        }
    }

    @Override
    protected MinecraftServerCommandSender createCommandSender(CommandSourceStack source) {
        return new FabricServerCommandSender(source);
    }

    @Override
    public TickHook createTickHook() {
        return new FabricTickHook.Server();
    }

    @Override
    public TickReporter createTickReporter() {
        return new FabricTickReporter.Server();
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new FabricClassSourceLookup(createClassFinder());
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                FabricLoader.getInstance().getAllMods(),
                mod -> mod.getMetadata().getId(),
                mod -> mod.getMetadata().getVersion().getFriendlyString(),
                mod -> mod.getMetadata().getAuthors().stream()
                        .map(Person::getName)
                        .collect(Collectors.joining(", ")),
                mod -> mod.getMetadata().getDescription()
        );
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new FabricWorldInfoProvider.Server(this.server);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new FabricPlatformInfo(PlatformInfo.Type.SERVER);
    }
}
