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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.minecraft.SparkMinecraftMod;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class MinecraftClientSparkPlugin<M extends SparkMinecraftMod, CS> extends MinecraftSparkPlugin<M> implements Command<CS>, SuggestionProvider<CS> {

    protected final Minecraft minecraft;
    private final ThreadDumper.GameThread gameThreadDumper;

    public MinecraftClientSparkPlugin(M mod, Minecraft minecraft, Supplier<Thread> gameThreadSupplier) {
        super(mod);
        this.minecraft = minecraft;
        this.gameThreadDumper = new ThreadDumper.GameThread(gameThreadSupplier);
    }

    protected abstract CommandSender createCommandSender(CS source);

    @Override
    public int run(CommandContext<CS> context) throws CommandSyntaxException {
        String[] args = processArgs(context, false, "sparkc", "sparkclient");
        if (args == null) {
            return 0;
        }

        this.platform.executeCommand(createCommandSender(context.getSource()), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CS> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String[] args = processArgs(context, true, "/sparkc", "/sparkclient");
        if (args == null) {
            return Suggestions.empty();
        }

        return generateSuggestions(createCommandSender(context.getSource()), args, builder);
    }

    @Override
    public void executeSync(Runnable task) {
        this.minecraft.executeIfPossible(task);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper.get();
    }

    @Override
    public String getCommandName() {
        return "sparkc";
    }

}
