package me.lucko.spark.sponge;

import com.google.inject.Inject;

import me.lucko.spark.common.CommandHandler;
import me.lucko.spark.profiler.ThreadDumper;
import me.lucko.spark.profiler.TickCounter;
import me.lucko.spark.sponge.utils.PomData;

import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.AsynchronousExecutor;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

@Plugin(
        id = "spark",
        name = "spark",
        version = "1.0.4",
        description = PomData.DESCRIPTION,
        authors = {"Luck", "sk89q"}
)
public class SparkSpongePlugin implements CommandCallable {

    private final CommandHandler<CommandSource> commandHandler = new CommandHandler<CommandSource>() {
        private Text colorize(String message) {
            return TextSerializers.FORMATTING_CODE.deserialize(message);
        }

        private void broadcast(Text msg) {
            Sponge.getServer().getConsole().sendMessage(msg);
            for (Player player : Sponge.getServer().getOnlinePlayers()) {
                if (player.hasPermission("spark.profiler")) {
                    player.sendMessage(msg);
                }
            }
        }

        @Override
        protected void sendMessage(CommandSource sender, String message) {
            sender.sendMessage(colorize(message));
        }

        @Override
        protected void sendMessage(String message) {
            Text msg = colorize(message);
            broadcast(msg);
        }

        @Override
        protected void sendLink(String url) {
            try {
                Text msg = Text.builder(url)
                        .color(TextColors.GRAY)
                        .onClick(TextActions.openUrl(new URL(url)))
                        .build();
                broadcast(msg);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void runAsync(Runnable r) {
            asyncExecutor.execute(r);
        }

        @Override
        protected ThreadDumper getDefaultThreadDumper() {
            return new ThreadDumper.Specific(new long[]{Thread.currentThread().getId()});
        }

        @Override
        protected TickCounter newTickCounter() {
            return new SpongeTickCounter(SparkSpongePlugin.this);
        }
    };

    @Inject
    @AsynchronousExecutor
    private SpongeExecutorService asyncExecutor;

    @Inject
    private Game game;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        game.getCommandManager().register(this, this, "spark", "profiler");
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) {
        if (!testPermission(source)) {
            source.sendMessage(Text.builder("You do not have permission to use this command.").color(TextColors.RED).build());
            return CommandResult.empty();
        }

        commandHandler.handleCommand(source, arguments.split(" "));
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
        return Collections.emptyList();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("spark.profiler");
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.of(Text.of("Main spark plugin command"));
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.of(Text.of("Run '/profiler' to view usage."));
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("Run '/profiler' to view usage.");
    }
}
