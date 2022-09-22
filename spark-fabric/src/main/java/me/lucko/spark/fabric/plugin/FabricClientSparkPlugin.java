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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import me.lucko.spark.common.platform.MetadataProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.fabric.FabricCommandSender;
import me.lucko.spark.fabric.FabricExtraMetadataProvider;
import me.lucko.spark.fabric.FabricPlatformInfo;
import me.lucko.spark.fabric.FabricSparkMod;
import me.lucko.spark.fabric.FabricTickHook;
import me.lucko.spark.fabric.FabricTickReporter;
import me.lucko.spark.fabric.FabricWorldInfoProvider;
import me.lucko.spark.fabric.mixin.MinecraftClientAccessor;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandOutput;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class FabricClientSparkPlugin extends FabricSparkPlugin implements Command<FabricClientCommandSource>, SuggestionProvider<FabricClientCommandSource> {

    public static void register(FabricSparkMod mod, MinecraftClient client) {
        FabricClientSparkPlugin plugin = new FabricClientSparkPlugin(mod, client);
        plugin.enable();
    }

    private final MinecraftClient minecraft;
    private final ThreadDumper.GameThread gameThreadDumper;

    public FabricClientSparkPlugin(FabricSparkMod mod, MinecraftClient minecraft) {
        super(mod);
        this.minecraft = minecraft;
        this.gameThreadDumper = new ThreadDumper.GameThread(() -> ((MinecraftClientAccessor) minecraft).getThread());
    }

    @Override
    public void enable() {
        super.enable();

        // events
        ClientLifecycleEvents.CLIENT_STOPPING.register(this::onDisable);
        ClientCommandRegistrationCallback.EVENT.register(this::onCommandRegister);
    }

    private void onDisable(MinecraftClient stoppingClient) {
        if (stoppingClient == this.minecraft) {
            disable();
        }
    }

    public void onCommandRegister(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        registerCommands(dispatcher, this, this, "sparkc", "sparkclient");
    }

    @Override
    public int run(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        String[] args = processArgs(context, false, "sparkc", "sparkclient");
        if (args == null) {
            return 0;
        }

        this.platform.executeCommand(new FabricCommandSender(context.getSource().getEntity(), this), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String[] args = processArgs(context, true, "/sparkc", "/sparkclient");
        if (args == null) {
            return Suggestions.empty();
        }

        return generateSuggestions(new FabricCommandSender(context.getSource().getEntity(), this), args, builder);
    }

    @Override
    public boolean hasPermission(CommandOutput sender, String permission) {
        return true;
    }

    @Override
    public Stream<FabricCommandSender> getCommandSenders() {
        return Stream.of(new FabricCommandSender(this.minecraft.player, this));
    }

    @Override
    public void executeSync(Runnable task) {
        this.minecraft.executeSync(task);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper.get();
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
    public MetadataProvider createExtraMetadataProvider() {
        return new FabricExtraMetadataProvider(this.minecraft.getResourcePackManager());
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new FabricWorldInfoProvider.Client(this.minecraft);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new FabricPlatformInfo(PlatformInfo.Type.CLIENT);
    }

    @Override
    public String getCommandName() {
        return "sparkc";
    }

}
