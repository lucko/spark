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
import me.lucko.spark.common.sampler.TickCounter;
import me.lucko.spark.fabric.FabricCommandSender;
import me.lucko.spark.fabric.FabricSparkMod;
import me.lucko.spark.fabric.FabricTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class FabricServerSparkPlugin extends FabricSparkPlugin implements Command<ServerCommandSource>, SuggestionProvider<ServerCommandSource> {

    public static void register(FabricSparkMod mod, MinecraftServer server) {
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();

        FabricServerSparkPlugin plugin = new FabricServerSparkPlugin(mod, server);
        registerCommands(dispatcher, plugin, plugin, "spark");
    }

    private final MinecraftServer server;

    public FabricServerSparkPlugin(FabricSparkMod mod, MinecraftServer server) {
        super(mod);
        this.server = server;
    }

    private String /*Nullable*/ [] processArgs(CommandContext<ServerCommandSource> context) {
        String[] split = context.getInput().split(" ");
        if (split.length == 0 || !split[0].equals("/spark")) {
            return null;
        }

        return Arrays.copyOfRange(split, 1, split.length);
    }

    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String[] args = processArgs(context);
        if (args == null)
            return 0;

        this.platform.executeCommand(new FabricCommandSender(context.getSource().getPlayer(), this), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder)
            throws CommandSyntaxException {
        String[] args = processArgs(context);
        if (args == null)
            return Suggestions.empty();
        ServerPlayerEntity player = context.getSource().getPlayer();

        return CompletableFuture.supplyAsync(() -> {
            for (String each : this.platform.tabCompleteCommand(new FabricCommandSender(player, this), args)) {
                builder.suggest(each);
            }
            return builder.build();
        });
    }

    @Override
    public boolean hasPermission(CommandOutput sender, String permission) {
        if (sender instanceof PlayerEntity) {
            return this.server.getPermissionLevel(((PlayerEntity) sender).getGameProfile()) >= 4;
        } else {
            return true;
        }
    }

    @Override
    public Stream<FabricCommandSender> getSendersWithPermission(String permission) {
        return Stream.concat(
                this.server.getPlayerManager().getPlayerList().stream()
                        .filter(player -> hasPermission(player, permission)),
                Stream.of(this.server)
        ).map(sender -> new FabricCommandSender(sender, this));
    }

    @Override
    public TickCounter createTickCounter() {
        return new FabricTickCounter.Server();
    }

    @Override
    public String getCommandName() {
        return "spark";
    }
}
