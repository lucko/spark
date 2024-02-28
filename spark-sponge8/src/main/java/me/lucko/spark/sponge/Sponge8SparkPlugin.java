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

import com.google.common.base.Suppliers;
import com.google.inject.Inject;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;

import net.kyori.adventure.text.Component;

import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.registrar.tree.CommandTreeNode;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import org.spongepowered.plugin.metadata.model.PluginContributor;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Plugin("spark")
public class Sponge8SparkPlugin implements SparkPlugin {

    private final PluginContainer pluginContainer;
    private final Logger logger;
    private final Game game;
    private final Path configDirectory;
    private final ExecutorService asyncExecutor;
    private final Supplier<ExecutorService> syncExecutor;
    private final ThreadDumper.GameThread gameThreadDumper = new ThreadDumper.GameThread();

    private SparkPlatform platform;

    @Inject
    public Sponge8SparkPlugin(PluginContainer pluginContainer, Logger logger, Game game, @ConfigDir(sharedRoot = false) Path configDirectory) {
        this.pluginContainer = pluginContainer;
        this.logger = logger;
        this.game = game;
        this.configDirectory = configDirectory;
        this.asyncExecutor = game.asyncScheduler().executor(pluginContainer);
        this.syncExecutor = Suppliers.memoize(() -> {
            if (this.game.isServerAvailable()) {
                return this.game.server().scheduler().executor(this.pluginContainer);
            } else if (this.game.isClientAvailable()) {
                return this.game.client().scheduler().executor(this.pluginContainer);
            } else {
                throw new IllegalStateException("Server and client both unavailable");
            }
        });
    }


    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Raw> event) {
        event.register(this.pluginContainer, new SparkCommand(this), this.pluginContainer.metadata().id());
    }

    @Listener
    public void onEnable(StartedEngineEvent<Server> event) {
        this.gameThreadDumper.setThread(Thread.currentThread());

        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    @Listener
    public void onDisable(StoppingEngineEvent<Server> event) {
        this.platform.disable();
    }

    @Override
    public String getVersion() {
        return this.pluginContainer.metadata().version().toString();
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
        if (this.game.isServerAvailable()) {
            return Stream.concat(
                    this.game.server().onlinePlayers().stream(),
                    Stream.of(this.game.systemSubject())
            ).map(Sponge8CommandSender::new);
        } else {
            return Stream.of(this.game.systemSubject()).map(Sponge8CommandSender::new);
        }
    }

    @Override
    public void executeAsync(Runnable task) {
        this.asyncExecutor.execute(task);
    }

    @Override
    public void executeSync(Runnable task) {
        this.syncExecutor.get().execute(task);
    }

    @Override
    public void log(Level level, String msg) {
        if (level == Level.INFO) {
            this.logger.info(msg);
        } else if (level == Level.WARNING) {
            this.logger.warn(msg);
        } else if (level == Level.SEVERE) {
            this.logger.error(msg);
        } else {
            throw new IllegalArgumentException(level.getName());
        }
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper.get();
    }

    @Override
    public TickHook createTickHook() {
        return new Sponge8TickHook(this.pluginContainer, this.game);
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new Sponge8ClassSourceLookup(this.game);
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                this.game.pluginManager().plugins(),
                plugin -> plugin.metadata().id(),
                plugin -> plugin.metadata().version().toString(),
                plugin -> plugin.metadata().contributors().stream()
                        .map(PluginContributor::name)
                        .collect(Collectors.joining(", "))
        );
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        if (this.game.isServerAvailable()) {
            return new Sponge8PlayerPingProvider(this.game.server());
        } else {
            return null;
        }
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        if (this.game.isServerAvailable()) {
            return new Sponge8WorldInfoProvider(this.game.server());
        } else {
            return WorldInfoProvider.NO_OP;
        }
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
            this.plugin.platform.executeCommand(new Sponge8CommandSender(cause), arguments.input().split(" "));
            return CommandResult.success();
        }

        @Override
        public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) {
            return this.plugin.platform.tabCompleteCommand(new Sponge8CommandSender(cause), arguments.input().split(" "))
                    .stream()
                    .map(CommandCompletion::of)
                    .collect(Collectors.toList());
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
