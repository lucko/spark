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

package me.lucko.spark.minestom;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionCallback;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.extensions.Extension;
import net.minestom.server.timer.ExecutionType;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.stream.Stream;

public class MinestomSparkPlugin extends Extension implements SparkPlugin {
    private SparkPlatform platform;
    private MinestomSparkCommand command;

    @Override
    public void initialize() {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
        this.command = new MinestomSparkCommand(this.platform);
        MinecraftServer.getCommandManager().register(this.command);
    }

    @Override
    public void terminate() {
        this.platform.disable();
        MinecraftServer.getCommandManager().unregister(this.command);
    }

    @Override
    public String getVersion() {
        return getOrigin().getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return getDataDirectory();
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<MinestomCommandSender> getCommandSenders() {
        return Stream.concat(
                MinecraftServer.getConnectionManager().getOnlinePlayers().stream(),
                Stream.of(MinecraftServer.getCommandManager().getConsoleSender())
        ).map(MinestomCommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(task, ExecutionType.ASYNC);
    }

    @Override
    public void log(Level level, String msg) {
        if (level == Level.INFO) {
            this.getLogger().info(msg);
        } else if (level == Level.WARNING) {
            this.getLogger().warn(msg);
        } else if (level == Level.SEVERE) {
            this.getLogger().error(msg);
        } else {
            throw new IllegalArgumentException(level.getName());
        }
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new MinestomPlatformInfo();
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new MinestomClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                MinecraftServer.getExtensionManager().getExtensions(),
                extension -> extension.getOrigin().getName(),
                extension -> extension.getOrigin().getVersion(),
                extension -> String.join(", ", extension.getOrigin().getAuthors())
        );
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new MinestomPlayerPingProvider();
    }

    @Override
    public TickReporter createTickReporter() {
        return new MinestomTickReporter();
    }

    @Override
    public TickHook createTickHook() {
        return new MinestomTickHook();
    }

    private static final class MinestomSparkCommand extends Command implements CommandExecutor, SuggestionCallback {
        private final SparkPlatform platform;

        public MinestomSparkCommand(SparkPlatform platform) {
            super("spark");
            this.platform = platform;

            ArgumentStringArray arrayArgument = ArgumentType.StringArray("args");
            arrayArgument.setSuggestionCallback(this);

            addSyntax(this, arrayArgument);
            setDefaultExecutor((sender, context) -> platform.executeCommand(new MinestomCommandSender(sender), new String[0]));
        }

        // execute
        @Override
        public void apply(@NotNull CommandSender sender, @NotNull CommandContext context) {
            String[] args = processArgs(context, false);
            if (args == null) {
                return;
            }

            this.platform.executeCommand(new MinestomCommandSender(sender), args);
        }

        // tab complete
        @Override
        public void apply(@NotNull CommandSender sender, @NotNull CommandContext context, @NotNull Suggestion suggestion) {
            String[] args = processArgs(context, true);
            if (args == null) {
                return;
            }

            Iterable<String> suggestionEntries = this.platform.tabCompleteCommand(new MinestomCommandSender(sender), args);
            for (String suggestionEntry : suggestionEntries) {
                suggestion.addEntry(new SuggestionEntry(suggestionEntry));
            }
        }

        private static String [] processArgs(CommandContext context, boolean tabComplete) {
            String[] split = context.getInput().split(" ", tabComplete ? -1 : 0);
            if (split.length == 0 || !split[0].equals("/spark") && !split[0].equals("spark")) {
                return null;
            }

            return Arrays.copyOfRange(split, 1, split.length);
        }
    }
}
