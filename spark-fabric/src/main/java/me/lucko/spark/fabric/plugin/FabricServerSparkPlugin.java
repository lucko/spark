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

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.MetadataProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.fabric.FabricCommandSender;
import me.lucko.spark.fabric.FabricExtraMetadataProvider;
import me.lucko.spark.fabric.FabricPlatformInfo;
import me.lucko.spark.fabric.FabricPlayerPingProvider;
import me.lucko.spark.fabric.FabricServerConfigProvider;
import me.lucko.spark.fabric.FabricSparkMod;
import me.lucko.spark.fabric.FabricTickHook;
import me.lucko.spark.fabric.FabricTickReporter;
import me.lucko.spark.fabric.FabricWorldInfoProvider;
import me.lucko.spark.fabric.placeholder.SparkFabricPlaceholderApi;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class FabricServerSparkPlugin extends FabricSparkPlugin implements Command<ServerCommandSource>, SuggestionProvider<ServerCommandSource> {

    public static FabricServerSparkPlugin register(FabricSparkMod mod, MinecraftServer server) {
        FabricServerSparkPlugin plugin = new FabricServerSparkPlugin(mod, server);
        plugin.enable();
        return plugin;
    }

    private final MinecraftServer server;
    private final ThreadDumper gameThreadDumper;

    public FabricServerSparkPlugin(FabricSparkMod mod, MinecraftServer server) {
        super(mod);
        this.server = server;
        this.gameThreadDumper = new ThreadDumper.Specific(server.getThread());
    }

    @Override
    public void enable() {
        super.enable();

        // register commands
        registerCommands(this.server.getCommandManager().getDispatcher());

        // placeholders
        if (FabricLoader.getInstance().isModLoaded("placeholder-api")) {
            try {
                SparkFabricPlaceholderApi.register(this.platform);
            } catch (LinkageError e) {
                // ignore
            }
        }
    }

    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        registerCommands(dispatcher, this, this, "spark");
    }

    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String[] args = processArgs(context, false, "/spark", "spark");
        if (args == null) {
            return 0;
        }

        CommandOutput source = context.getSource().getEntity() != null ? context.getSource().getEntity() : context.getSource().getServer();
        this.platform.executeCommand(new FabricCommandSender(source, this), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String[] args = processArgs(context, true, "/spark", "spark");
        if (args == null) {
            return Suggestions.empty();
        }

        return generateSuggestions(new FabricCommandSender(context.getSource().getPlayer(), this), args, builder);
    }

    @Override
    public boolean hasPermission(CommandOutput sender, String permission) {
        if (sender instanceof PlayerEntity player) {
            return Permissions.getPermissionValue(player, permission).orElseGet(() -> {
                MinecraftServer server = player.getServer();
                if (server != null && server.isHost(player.getGameProfile())) {
                    return true;
                }

                return player.hasPermissionLevel(4);
            });
        } else {
            return true;
        }
    }

    @Override
    public Stream<FabricCommandSender> getCommandSenders() {
        return Stream.concat(
                this.server.getPlayerManager().getPlayerList().stream(),
                Stream.of(this.server)
        ).map(sender -> new FabricCommandSender(sender, this));
    }

    @Override
    public void executeSync(Runnable task) {
        this.server.executeSync(task);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
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
    public PlayerPingProvider createPlayerPingProvider() {
        return new FabricPlayerPingProvider(this.server);
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new FabricServerConfigProvider();
    }

    @Override
    public MetadataProvider createExtraMetadataProvider() {
        return new FabricExtraMetadataProvider(this.server.getDataPackManager());
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new FabricWorldInfoProvider.Server(this.server);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new FabricPlatformInfo(PlatformInfo.Type.SERVER);
    }

    @Override
    public String getCommandName() {
        return "spark";
    }
}
