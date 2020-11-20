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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.forge.ForgeSparkMod;
import net.minecraft.command.ICommandSource;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class ForgeSparkPlugin implements SparkPlugin {

    public static <T> void registerCommands(CommandDispatcher<T> dispatcher, Command<T> executor, SuggestionProvider<T> suggestor, String... aliases) {
        if (aliases.length == 0) {
            return;
        }

        String mainName = aliases[0];
        LiteralArgumentBuilder<T> command = LiteralArgumentBuilder.<T>literal(mainName)
                .executes(executor)
                .then(RequiredArgumentBuilder.<T, String>argument("args", StringArgumentType.greedyString())
                        .suggests(suggestor)
                        .executes(executor)
                );

        LiteralCommandNode<T> node = dispatcher.register(command);
        for (int i = 1; i < aliases.length; i++) {
            dispatcher.register(LiteralArgumentBuilder.<T>literal(aliases[i]).redirect(node));
        }
    }

    private final ForgeSparkMod mod;
    protected final ScheduledExecutorService scheduler;
    protected final SparkPlatform platform;

    protected ForgeSparkPlugin(ForgeSparkMod mod) {
        this.mod = mod;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("spark-forge-async-worker");
            thread.setDaemon(true);
            return thread;
        });
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    public void enable() {
        this.platform.enable();
    }

    public void disable() {
        this.platform.disable();
        this.scheduler.shutdown();
    }

    public abstract boolean hasPermission(ICommandSource sender, String permission);

    @Override
    public String getVersion() {
        return this.mod.getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return this.mod.getConfigDirectory();
    }

    @Override
    public void executeAsync(Runnable task) {
        this.scheduler.execute(task);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return new ThreadDumper.Specific(new long[]{Thread.currentThread().getId()});
    }

}
