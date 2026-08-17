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

import com.mojang.brigadier.CommandDispatcher;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.fabric.FabricClassSourceLookup;
import me.lucko.spark.fabric.FabricClientCommandSender;
import me.lucko.spark.fabric.FabricPlatformInfo;
import me.lucko.spark.fabric.FabricSparkMod;
import me.lucko.spark.fabric.FabricTickHook;
import me.lucko.spark.fabric.FabricTickReporter;
import me.lucko.spark.fabric.FabricWorldInfoProvider;
import me.lucko.spark.fabric.mixin.MinecraftAccessor;
import me.lucko.spark.minecraft.plugin.MinecraftClientSparkPlugin;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandBuildContext;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FabricClientSparkPlugin extends MinecraftClientSparkPlugin<FabricSparkMod, FabricClientCommandSource> {

    public static void init(FabricSparkMod mod, Minecraft client) {
        FabricClientSparkPlugin plugin = new FabricClientSparkPlugin(mod, client);
        plugin.enable();
    }

    public FabricClientSparkPlugin(FabricSparkMod mod, Minecraft minecraft) {
        super(mod, minecraft, ((MinecraftAccessor) minecraft)::getGameThread);
    }

    @Override
    public void enable() {
        super.enable();

        // events
        ClientLifecycleEvents.CLIENT_STOPPING.register(this::onDisable);
        ClientCommandRegistrationCallback.EVENT.register(this::onCommandRegister);
    }

    private void onDisable(Minecraft stoppingClient) {
        if (stoppingClient == this.minecraft) {
            disable();
        }
    }

    private void onCommandRegister(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext ctx) {
        registerCommands(dispatcher, this, this, "sparkc", "sparkclient");
    }

    @Override
    protected CommandSender createCommandSender(FabricClientCommandSource source) {
        return new FabricClientCommandSender(source);
    }

    @Override
    public Stream<CommandSender> getCommandSenders() {
        ClientPacketListener listener = this.minecraft.getConnection();
        if (listener == null) {
            return Stream.empty();
        }
        return Stream.of(new FabricClientCommandSender(listener.getSuggestionsProvider()));
    }

    @Override
    public TickHook createTickHook() {
        return new FabricTickHook.Client();
    }

    @Override
    public TickReporter createTickReporter() {
        return new FabricTickReporter.Client();
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
        return new FabricWorldInfoProvider.Client(this.minecraft);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new FabricPlatformInfo(PlatformInfo.Type.CLIENT);
    }

}
