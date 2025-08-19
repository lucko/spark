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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.forge.ForgeClientCommandSender;
import me.lucko.spark.forge.ForgePlatformInfo;
import me.lucko.spark.forge.ForgeSparkMod;
import me.lucko.spark.forge.ForgeTickHook;
import me.lucko.spark.forge.ForgeTickReporter;
import me.lucko.spark.forge.ForgeWorldInfoProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ForgeClientSparkPlugin extends ForgeSparkPlugin implements Command<CommandSourceStack>, SuggestionProvider<CommandSourceStack> {

    public static void register(ForgeSparkMod mod, FMLClientSetupEvent event) {
        ForgeClientSparkPlugin plugin = new ForgeClientSparkPlugin(mod, Minecraft.getInstance());
        plugin.enable();
    }

    private final Minecraft minecraft;
    private final ThreadDumper gameThreadDumper;
    private Collection<EventListener> listeners = Collections.emptyList();

    public ForgeClientSparkPlugin(ForgeSparkMod mod, Minecraft minecraft) {
        super(mod);
        this.minecraft = minecraft;
        this.gameThreadDumper = new ThreadDumper.Specific(minecraft.gameThread);
    }

    @Override
    public void enable() {
        super.enable();

        // register listeners
        this.listeners = BusGroup.DEFAULT.register(MethodHandles.lookup(), this);
    }

    @Override
    public void disable() {
        super.disable();

        // unregister listeners
        if (!this.listeners.isEmpty()) {
            BusGroup.DEFAULT.unregister(this.listeners);
        }
        this.listeners = Collections.emptyList();
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterClientCommandsEvent e) {
        registerCommands(e.getDispatcher(), this, this, "sparkc", "sparkclient");
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String[] args = processArgs(context, false, "sparkc", "sparkclient");
        if (args == null) {
            return 0;
        }

        this.platform.executeCommand(new ForgeClientCommandSender(context.getSource()), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String[] args = processArgs(context, true, "/sparkc", "/sparkclient");
        if (args == null) {
            return Suggestions.empty();
        }

        return generateSuggestions(new ForgeClientCommandSender(context.getSource()), args, builder);
    }

    @Override
    public Stream<ForgeClientCommandSender> getCommandSenders() {
        return Stream.of(new ForgeClientCommandSender(ClientCommandHandler.getSource()));
    }

    @Override
    public void executeSync(Runnable task) {
        this.minecraft.executeIfPossible(task);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
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
    public WorldInfoProvider createWorldInfoProvider() {
        return new ForgeWorldInfoProvider.Client(this.minecraft);
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
