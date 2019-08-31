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
import me.lucko.spark.fabric.FabricSparkGameHooks;
import me.lucko.spark.fabric.FabricSparkMod;
import me.lucko.spark.fabric.FabricTickCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.CommandSource;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class FabricClientSparkPlugin extends FabricSparkPlugin implements SuggestionProvider<CommandSource> {

    public static void register(FabricSparkMod mod, MinecraftClient client) {
        FabricClientSparkPlugin plugin = new FabricClientSparkPlugin(mod, client);

        plugin.scheduler.scheduleWithFixedDelay(plugin::checkCommandRegistered, 10, 10, TimeUnit.SECONDS);
    }

    private final MinecraftClient minecraft;
    private CommandDispatcher<CommandSource> dispatcher;

    public FabricClientSparkPlugin(FabricSparkMod mod, MinecraftClient minecraft) {
        super(mod);
        this.minecraft = minecraft;
    }

    private void checkCommandRegistered() {
        ClientPlayerEntity player = this.minecraft.player;
        if (player == null) {
            return;
        }

        ClientPlayNetworkHandler connection = player.networkHandler;
        if (connection == null) {
            return;
        }

        try {
            CommandDispatcher<CommandSource> dispatcher = connection.getCommandDispatcher();
            if (dispatcher != this.dispatcher) {
                this.dispatcher = dispatcher;
                registerCommands(this.dispatcher, c -> Command.SINGLE_SUCCESS, this, "sparkc", "sparkclient");
                FabricSparkGameHooks.INSTANCE.setChatSendCallback(this::onClientChat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onClientChat(String chat) {
        String[] split = chat.split(" ");
        if (split.length == 0 || (!split[0].equals("/sparkc") && !split[0].equals("/sparkclient"))) {
            return false;
        }

        String[] args = Arrays.copyOfRange(split, 1, split.length);
        this.platform.executeCommand(new FabricCommandSender(this.minecraft.player, this), args);
        this.minecraft.inGameHud.getChatHud().addToMessageHistory(chat);
        return true;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder builder)
            throws CommandSyntaxException {
        String[] split = context.getInput().split(" ");
        if (split.length == 0 || (!split[0].equals("/sparkc") && !split[0].equals("/sparkclient"))) {
            return Suggestions.empty();
        }

        String[] args = Arrays.copyOfRange(split, 1, split.length);

        return CompletableFuture.supplyAsync(() -> {
            for (String each : this.platform.tabCompleteCommand(new FabricCommandSender(this.minecraft.player, this), args)) {
                builder.suggest(each);
            }
            return builder.build();
        });
    }

    @Override
    public boolean hasPermission(CommandOutput sender, String permission) {
        return true;
    }

    @Override
    public Stream<FabricCommandSender> getSendersWithPermission(String permission) {
        return Stream.of(new FabricCommandSender(this.minecraft.player, this));
    }

    @Override
    public TickCounter createTickCounter() {
        return new FabricTickCounter.Client();
    }

    @Override
    public String getCommandName() {
        return "sparkc";
    }

}
