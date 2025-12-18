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

package me.lucko.spark.minecraft.plugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.minecraft.MinecraftPlayerPingProvider;
import me.lucko.spark.minecraft.MinecraftServerConfigProvider;
import me.lucko.spark.minecraft.SparkMinecraftMod;
import me.lucko.spark.minecraft.sender.MinecraftServerCommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public abstract class MinecraftServerSparkPlugin<M extends SparkMinecraftMod> extends MinecraftSparkPlugin<M> implements Command<CommandSourceStack>, SuggestionProvider<CommandSourceStack> {

    protected final MinecraftServer server;
    private final ThreadDumper gameThreadDumper;

    public MinecraftServerSparkPlugin(M mod, MinecraftServer server) {
        super(mod);
        this.server = server;
        this.gameThreadDumper = new ThreadDumper.Specific(server.getRunningThread());
    }

    @Override
    public void enable() {
        super.enable();

        // register commands
        registerCommands(this.server.getCommands().getDispatcher());
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerCommands(dispatcher, this, this, "spark");
    }

    protected abstract MinecraftServerCommandSender createCommandSender(CommandSourceStack source);

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String[] args = processArgs(context, false, "/spark", "spark");
        if (args == null) {
            return 0;
        }

        this.platform.executeCommand(createCommandSender(context.getSource()), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String[] args = processArgs(context, true, "/spark", "spark");
        if (args == null) {
            return Suggestions.empty();
        }

        return generateSuggestions(createCommandSender(context.getSource()), args, builder);
    }

    @Override
    public Stream<MinecraftServerCommandSender> getCommandSenders() {
        return Stream.concat(
                this.server.getPlayerList().getPlayers().stream().map(ServerPlayer::createCommandSourceStack),
                Stream.of(this.server.createCommandSourceStack())
        ).map(this::createCommandSender);
    }

    @Override
    public void executeSync(Runnable task) {
        this.server.executeIfPossible(task);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new MinecraftPlayerPingProvider(this.server);
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new MinecraftServerConfigProvider();
    }

    @Override
    public String getCommandName() {
        return "spark";
    }
}
