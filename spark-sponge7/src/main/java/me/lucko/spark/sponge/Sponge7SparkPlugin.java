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

import me.lucko.spark.api.Spark;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.tick.TickHook;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.AsynchronousExecutor;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.SynchronousExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.annotation.Nullable;

@Plugin(
        id = "spark",
        name = "spark",
        version = "@version@",
        description = "@desc@",
        authors = {"Luck"}
)
public class Sponge7SparkPlugin implements SparkPlugin {

    private final PluginContainer pluginContainer;
    private final Logger logger;
    private final Game game;
    private final Path configDirectory;
    private final SpongeExecutorService asyncExecutor;
    private final SpongeExecutorService syncExecutor;
    private final ThreadDumper.GameThread gameThreadDumper = new ThreadDumper.GameThread();

    private SparkPlatform platform;

    @Inject
    public Sponge7SparkPlugin(PluginContainer pluginContainer, Logger logger, Game game, @ConfigDir(sharedRoot = false) Path configDirectory, @AsynchronousExecutor SpongeExecutorService asyncExecutor, @SynchronousExecutor SpongeExecutorService syncExecutor) {
        this.pluginContainer = pluginContainer;
        this.logger = logger;
        this.game = game;
        this.configDirectory = configDirectory;
        this.asyncExecutor = asyncExecutor;
        this.syncExecutor = syncExecutor;

        this.syncExecutor.execute(() -> this.gameThreadDumper.setThread(Thread.currentThread()));
    }

    @Listener
    public void onEnable(GameStartedServerEvent event) {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
        this.game.getCommandManager().register(this, new SparkCommand(this), "spark");
    }

    @Listener
    public void onDisable(GameStoppingServerEvent event) {
        this.platform.disable();
    }

    @Override
    public String getVersion() {
        return Sponge7SparkPlugin.class.getAnnotation(Plugin.class).version();
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
    public Stream<Sponge7CommandSender> getCommandSenders() {
        if (this.game.isServerAvailable()) {
            return Stream.concat(
                    this.game.getServer().getOnlinePlayers().stream(),
                    Stream.of(this.game.getServer().getConsole())
            ).map(Sponge7CommandSender::new);
        } else {
            return Stream.of(this.game.getServer().getConsole()).map(Sponge7CommandSender::new);
        }
    }

    @Override
    public void executeAsync(Runnable task) {
        this.asyncExecutor.execute(task);
    }

    @Override
    public void executeSync(Runnable task) {
        this.syncExecutor.execute(task);
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
        return new Sponge7TickHook(this);
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new Sponge7ClassSourceLookup(this.game);
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        if (this.game.isServerAvailable()) {
            return new Sponge7PlayerPingProvider(this.game.getServer());
        } else {
            return null;
        }
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        if (this.game.isServerAvailable()) {
            return new Sponge7WorldInfoProvider(this.game.getServer());
        } else {
            return WorldInfoProvider.NO_OP;
        }
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new Sponge7PlatformInfo(this.game);
    }

    @Override
    public void registerApi(Spark api) {
        this.game.getServiceManager().setProvider(this, Spark.class, api);
    }

    private static final class SparkCommand implements CommandCallable {
        private final Sponge7SparkPlugin plugin;

        private SparkCommand(Sponge7SparkPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public CommandResult process(CommandSource source, String arguments) {
            this.plugin.platform.executeCommand(new Sponge7CommandSender(source), arguments.split(" "));
            return CommandResult.empty();
        }

        @Override
        public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
            return this.plugin.platform.tabCompleteCommand(new Sponge7CommandSender(source), arguments.split(" "));
        }

        @Override
        public boolean testPermission(CommandSource source) {
            return this.plugin.platform.hasPermissionForAnyCommand(new Sponge7CommandSender(source));
        }

        @Override
        public Optional<Text> getShortDescription(CommandSource source) {
            return Optional.of(Text.of("Main spark plugin command"));
        }

        @Override
        public Optional<Text> getHelp(CommandSource source) {
            return Optional.of(Text.of("Run '/spark' to view usage."));
        }

        @Override
        public Text getUsage(CommandSource source) {
            return Text.of("Run '/spark' to view usage.");
        }
    }
}
