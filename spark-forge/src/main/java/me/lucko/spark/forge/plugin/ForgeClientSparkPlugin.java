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
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.tick.TickHook;
import me.lucko.spark.common.sampler.tick.TickReporter;
import me.lucko.spark.forge.ForgeCommandSender;
import me.lucko.spark.forge.ForgePlatformInfo;
import me.lucko.spark.forge.ForgeSparkMod;
import me.lucko.spark.forge.ForgeTickHook;
import me.lucko.spark.forge.ForgeTickReporter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ForgeClientSparkPlugin extends ForgeSparkPlugin implements SuggestionProvider<ISuggestionProvider> {

    public static void register(ForgeSparkMod mod, FMLClientSetupEvent event) {
        ForgeClientSparkPlugin plugin = new ForgeClientSparkPlugin(mod, event.getMinecraftSupplier().get());
        plugin.enable();

        // register listeners
        MinecraftForge.EVENT_BUS.register(plugin);

        // ensure commands are registered
        plugin.scheduler.scheduleWithFixedDelay(plugin::checkCommandRegistered, 10, 10, TimeUnit.SECONDS);
    }

    private final Minecraft minecraft;
    private CommandDispatcher<ISuggestionProvider> dispatcher;

    public ForgeClientSparkPlugin(ForgeSparkMod mod, Minecraft minecraft) {
        super(mod);
        this.minecraft = minecraft;
    }

    private CommandDispatcher<ISuggestionProvider> getPlayerCommandDispatcher() {
        return Optional.ofNullable(this.minecraft.player)
                .map(player -> player.connection)
                .map(ClientPlayNetHandler::getCommandDispatcher)
                .orElse(null);
    }

    private void checkCommandRegistered() {
        CommandDispatcher<ISuggestionProvider> dispatcher = getPlayerCommandDispatcher();
        if (dispatcher == null) {
            return;
        }

        try {
            if (dispatcher != this.dispatcher) {
                this.dispatcher = dispatcher;
                registerCommands(this.dispatcher, context -> Command.SINGLE_SUCCESS, this, "sparkc", "sparkclient");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        String chat = event.getMessage();
        String[] split = chat.split(" ");
        if (split.length == 0 || (!split[0].equals("/sparkc") && !split[0].equals("/sparkclient"))) {
            return;
        }

        String[] args = Arrays.copyOfRange(split, 1, split.length);
        this.platform.executeCommand(new ForgeCommandSender(this.minecraft.player, this), args);
        this.minecraft.ingameGUI.getChatGUI().addToSentMessages(chat);
        event.setCanceled(true);
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ISuggestionProvider> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String chat = context.getInput();
        String[] split = chat.split(" ");
        if (split.length == 0 || (!split[0].equals("/sparkc") && !split[0].equals("/sparkclient"))) {
            return Suggestions.empty();
        }

        String[] args = Arrays.copyOfRange(split, 1, split.length);

        return CompletableFuture.supplyAsync(() -> {
            for (String suggestion : this.platform.tabCompleteCommand(new ForgeCommandSender(this.minecraft.player, this), args)) {
                builder.suggest(suggestion);
            }
            return builder.build();
        });
    }

    @Override
    public boolean hasPermission(ICommandSource sender, String permission) {
        return true;
    }

    @Override
    public Stream<ForgeCommandSender> getSendersWithPermission(String permission) {
        return Stream.of(new ForgeCommandSender(this.minecraft.player, this));
    }

    @Override
    public TickHook createTickHook() {
        return new ForgeTickHook(TickEvent.Type.CLIENT);
    }

    @Override
    public TickReporter createTickReporter() {
        return new ForgeTickReporter(TickEvent.Type.CLIENT);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new ForgePlatformInfo(PlatformInfo.Type.CLIENT);
    }

    @Override
    public String getCommandName() {
        return "sparkc";
    }

}
