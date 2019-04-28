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
import me.lucko.spark.common.CommandSender;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.TickCounter;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.AsynchronousExecutor;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Plugin(
        id = "spark",
        name = "spark",
        version = "@version@",
        description = "@desc@",
        authors = {"Luck", "sk89q"},
        dependencies = {
                // explicit dependency on spongeapi with no defined API version
                @Dependency(id = "spongeapi")
        }
)
public class SparkSpongePlugin implements SparkPlugin {

    private final Game game;
    private final Path configDirectory;
    private final SpongeExecutorService asyncExecutor;

    private final SparkPlatform platform = new SparkPlatform(this);

    @Inject
    public SparkSpongePlugin(Game game, @ConfigDir(sharedRoot = false) Path configDirectory, @AsynchronousExecutor SpongeExecutorService asyncExecutor) {
        this.game = game;
        this.configDirectory = configDirectory;
        this.asyncExecutor = asyncExecutor;
    }

    @Listener
    public void onEnable(GameStartedServerEvent event) {
        this.platform.enable();
        this.game.getCommandManager().register(this, new SparkCommand(this), "spark");
    }

    @Listener
    public void onDisable(GameStoppingServerEvent event) {
        this.platform.disable();
    }

    @Override
    public String getVersion() {
        return SparkSpongePlugin.class.getAnnotation(Plugin.class).version();
    }

    @Override
    public Path getPluginFolder() {
        return this.configDirectory;
    }

    @Override
    public String getLabel() {
        return "spark";
    }

    @Override
    public Set<CommandSender> getSendersWithPermission(String permission) {
        List<CommandSource> senders = new LinkedList<>(this.game.getServer().getOnlinePlayers());
        senders.removeIf(sender -> !sender.hasPermission(permission));
        senders.add(this.game.getServer().getConsole());
        return senders.stream().map(SpongeCommandSender::new).collect(Collectors.toSet());
    }

    @Override
    public void runAsync(Runnable r) {
        this.asyncExecutor.execute(r);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return new ThreadDumper.Specific(new long[]{Thread.currentThread().getId()});
    }

    @Override
    public TickCounter createTickCounter() {
        return new SpongeTickCounter(this);
    }

    private static final class SparkCommand implements CommandCallable {
        private final SparkSpongePlugin plugin;

        private SparkCommand(SparkSpongePlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public CommandResult process(CommandSource source, String arguments) {
            this.plugin.platform.executeCommand(new SpongeCommandSender(source), arguments.split(" "));
            return CommandResult.empty();
        }

        @Override
        public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
            return this.plugin.platform.tabCompleteCommand(new SpongeCommandSender(source), arguments.split(" "));
        }

        @Override
        public boolean testPermission(CommandSource source) {
            return source.hasPermission("spark");
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
