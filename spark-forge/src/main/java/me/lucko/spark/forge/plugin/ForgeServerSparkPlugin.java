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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.spark.common.PlatformInfo;
import me.lucko.spark.common.sampler.tick.TickHook;
import me.lucko.spark.common.sampler.tick.TickReporter;
import me.lucko.spark.forge.*;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ForgeServerSparkPlugin extends ForgeSparkPlugin implements Command<CommandSource>, SuggestionProvider<CommandSource> {

    public static void register(ForgeSparkMod mod, FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        ForgeServerSparkPlugin plugin = new ForgeServerSparkPlugin(mod, server);

        CommandDispatcher<CommandSource> dispatcher = event.getCommandDispatcher();
        registerCommands(dispatcher, plugin, plugin, "spark");
        PermissionAPI.registerNode("spark", DefaultPermissionLevel.OP, "Access to the spark command");
    }

    private final MinecraftServer server;

    public ForgeServerSparkPlugin(ForgeSparkMod mod, MinecraftServer server) {
        super(mod);
        this.server = server;
    }

    private static String /* Nullable */[] processArgs(CommandContext<CommandSource> context) {
        String[] split = context.getInput().split(" ");
        if (split.length == 0 || !split[0].equals("/spark") && !split[0].equals("spark")) {
            return null;
        }

        return Arrays.copyOfRange(split, 1, split.length);
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String[] args = processArgs(context);
        if (args == null)
            return 0;
        this.platform.executeCommand(new ForgeCommandSender(context.getSource().source, this), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String[] args = processArgs(context);
        if (args == null) {
            return Suggestions.empty();
        }

        ServerPlayerEntity player = context.getSource().asPlayer();
        return CompletableFuture.supplyAsync(() -> {
            for (String suggestion : this.platform.tabCompleteCommand(new ForgeCommandSender(player, this), args)) {
                builder.suggest(suggestion);
            }
            return builder.build();
        });
    }

    @Override
    public boolean hasPermission(ICommandSource sender, String permission) {
        if (sender instanceof PlayerEntity) {
            return PermissionAPI.hasPermission((PlayerEntity) sender, permission);
        } else {
            return true;
        }
    }

    @Override
    public Stream<ForgeCommandSender> getSendersWithPermission(String permission) {
        return Stream.concat(
                this.server.getPlayerList().getPlayers().stream().filter(player -> hasPermission(player, permission)),
                Stream.of(this.server)
        ).map(sender -> new ForgeCommandSender(sender, this));
    }

    @Override
    public TickHook createTickHook() {
        return new ForgeTickHook(TickEvent.Type.SERVER);
    }

    @Override
    public TickReporter createTickReporter() {
        return new ForgeTickReporter(TickEvent.Type.SERVER);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new ForgePlatformInfo(PlatformInfo.Type.SERVER);
    }

    @Override
    public String getCommandName() {
        return "spark";
    }
}
