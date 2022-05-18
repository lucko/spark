package me.lucko.spark.minestom;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.util.ClassSourceLookup;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
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
        return "sparkms";
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
            super("sparkms");
            var arrayArgument = ArgumentType.StringArray("query");
            arrayArgument.setSuggestionCallback((sender, context, suggestion) -> {
                var args = context.get(arrayArgument);
                if (args == null) {
                    args = new String[0];
                }
                Iterable<String> suggestionEntries = extension.platform.tabCompleteCommand(new MinestomCommandSender(sender), args);
                for (var suggestionEntry : suggestionEntries) {
                    suggestion.addEntry(new SuggestionEntry(suggestionEntry));
                }
            });
            addSyntax((sender, context) -> {
                var args = context.get(arrayArgument);
                if (args == null) {
                    args = new String[0];
                }
                extension.platform.executeCommand(new MinestomCommandSender(sender), args);
            }, arrayArgument);
        }
    }
}
