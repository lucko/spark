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
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.common.util.ClassSourceLookup;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.extensions.Extension;
import net.minestom.server.timer.ExecutionType;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.stream.Stream;

public class MinestomSparkExtension extends Extension implements SparkPlugin {
    private SparkPlatform platform;
    private MinestomSparkCommand command;

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
    public Stream<? extends CommandSender> getCommandSenders() {
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

    @Override
    public void initialize() {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
        this.command = new MinestomSparkCommand(this);
        MinecraftServer.getCommandManager().register(command);
    }

    @Override
    public void terminate() {
        this.platform.disable();
        MinecraftServer.getCommandManager().unregister(command);
    }

    private static final class MinestomSparkCommand extends Command {
        public MinestomSparkCommand(MinestomSparkExtension extension) {
            super("spark", "sparkms");
            setDefaultExecutor((sender, context) -> extension.platform.executeCommand(new MinestomCommandSender(sender), new String[0]));
            ArgumentStringArray arrayArgument = ArgumentType.StringArray("query");
            arrayArgument.setSuggestionCallback((sender, context, suggestion) -> {
                String[] args = context.get(arrayArgument);
                if (args == null) {
                    args = new String[0];
                }
                Iterable<String> suggestionEntries = extension.platform.tabCompleteCommand(new MinestomCommandSender(sender), args);
                for (String suggestionEntry : suggestionEntries) {
                    suggestion.addEntry(new SuggestionEntry(suggestionEntry));
                }
            });
            addSyntax((sender, context) -> {
                String[] args = context.get(arrayArgument);
                if (args == null) {
                    args = new String[0];
                }
                extension.platform.executeCommand(new MinestomCommandSender(sender), args);
            }, arrayArgument);
        }
    }
}
