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

package me.lucko.spark.sponge;

import com.google.inject.Inject;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.tick.TickHook;

import net.kyori.adventure.text.Component;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.registrar.tree.CommandTreeNode;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

@Plugin("spark")
public class Sponge8SparkPlugin implements SparkPlugin {

    private final PluginContainer pluginContainer;
    private final Game game;
    private final Path configDirectory;
    private final ExecutorService asyncExecutor;

    private SparkPlatform platform;
    private final ThreadDumper.GameThread threadDumper = new ThreadDumper.GameThread();

    @Inject
    public Sponge8SparkPlugin(PluginContainer pluginContainer, Game game, @ConfigDir(sharedRoot = false) Path configDirectory) {
        this.pluginContainer = pluginContainer;
        this.game = game;
        this.configDirectory = configDirectory;
        this.asyncExecutor = game.asyncScheduler().createExecutor(pluginContainer);
    }


    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Raw> event) {
        event.register(this.pluginContainer, new SparkCommand(this), this.pluginContainer.getMetadata().getId());
    }

    @Listener
    public void onEnable(StartedEngineEvent<Server> event) {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    @Listener
    public void onDisable(StoppingEngineEvent<Server> event) {
        this.platform.disable();
    }

    @Override
    public String getVersion() {
        return this.pluginContainer.getMetadata().getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return this.configDirectory;
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<CommandSender> getCommandSenders() {
        return Stream.concat(
                this.game.server().onlinePlayers().stream(),
                Stream.of(this.game.systemSubject())
        ).map(Sponge8CommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        this.asyncExecutor.execute(task);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.threadDumper.get();
    }

    @Override
    public TickHook createTickHook() {
        return new Sponge8TickHook(this.pluginContainer, this.game);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new Sponge8PlatformInfo(this.game);
    }

    private static final class SparkCommand implements Command.Raw {
        private final Sponge8SparkPlugin plugin;

        public SparkCommand(Sponge8SparkPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public CommandResult process(CommandCause cause, ArgumentReader.Mutable arguments) {
            this.plugin.threadDumper.ensureSetup();
            this.plugin.platform.executeCommand(new Sponge8CommandSender(cause), arguments.input().split(" "));
            return CommandResult.empty();
        }

        @Override
        public List<String> suggestions(CommandCause cause, ArgumentReader.Mutable arguments) {
            return this.plugin.platform.tabCompleteCommand(new Sponge8CommandSender(cause), arguments.input().split(" "));
        }

        @Override
        public boolean canExecute(CommandCause cause) {
            return this.plugin.platform.hasPermissionForAnyCommand(new Sponge8CommandSender(cause));
        }

        @Override
        public Optional<Component> shortDescription(CommandCause cause) {
            return Optional.of(Component.text("Main spark plugin command"));
        }

        @Override
        public Optional<Component> extendedDescription(CommandCause cause) {
            return Optional.empty();
        }

        @Override
        public Component usage(CommandCause cause) {
            return Component.text("Run '/spark' to view usage.");
        }

        @Override
        public CommandTreeNode.Root commandTree() {
            return Command.Raw.super.commandTree();
        }

        @Override
        public Optional<Component> help(@NonNull CommandCause cause) {
            return Optional.of(Component.text("Run '/spark' to view usage."));
        }
    }
}
