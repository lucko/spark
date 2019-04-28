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

package me.lucko.spark.common;

import com.google.common.collect.ImmutableList;
import me.lucko.spark.common.command.Arguments;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.command.modules.ActivityLogModule;
import me.lucko.spark.common.command.modules.HealthModule;
import me.lucko.spark.common.command.modules.MemoryModule;
import me.lucko.spark.common.command.modules.SamplerModule;
import me.lucko.spark.common.command.modules.TickMonitoringModule;
import me.lucko.spark.common.command.tabcomplete.CompletionSupplier;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.monitor.tick.TpsCalculator;
import me.lucko.spark.common.sampler.TickCounter;
import me.lucko.spark.common.util.BytebinClient;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract spark implementation used by all platforms.
 */
public class SparkPlatform {

    /** The URL of the viewer frontend */
    public static final String VIEWER_URL = "https://sparkprofiler.github.io/#";
    /** The shared okhttp client */
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();
    /** The bytebin instance used by the platform */
    public static final BytebinClient BYTEBIN_CLIENT = new BytebinClient(OK_HTTP_CLIENT, "https://bytebin.lucko.me/", "spark-plugin");

    private final SparkPlugin plugin;
    private final List<Command> commands;
    private final ActivityLog activityLog;
    private final TickCounter tickCounter;
    private final TpsCalculator tpsCalculator;

    public SparkPlatform(SparkPlugin plugin) {
        this.plugin = plugin;

        ImmutableList.Builder<Command> commandsBuilder = ImmutableList.builder();
        new SamplerModule().registerCommands(commandsBuilder::add);
        new HealthModule().registerCommands(commandsBuilder::add);
        new TickMonitoringModule().registerCommands(commandsBuilder::add);
        new MemoryModule().registerCommands(commandsBuilder::add);
        new ActivityLogModule().registerCommands(commandsBuilder::add);
        this.commands = commandsBuilder.build();

        this.activityLog = new ActivityLog(plugin.getPluginFolder().resolve("activity.json"));
        this.activityLog.load();

        this.tickCounter = plugin.createTickCounter();
        this.tpsCalculator = this.tickCounter != null ? new TpsCalculator() : null;
    }

    public void enable() {
        if (this.tickCounter != null) {
            this.tickCounter.addTickTask(this.tpsCalculator);
            this.tickCounter.start();
        }
    }

    public void disable() {
        if (this.tickCounter != null) {
            this.tickCounter.close();
        }
    }

    public SparkPlugin getPlugin() {
        return this.plugin;
    }

    public ActivityLog getActivityLog() {
        return this.activityLog;
    }

    public TickCounter getTickCounter() {
        return this.tickCounter;
    }

    public TpsCalculator getTpsCalculator() {
        return this.tpsCalculator;
    }

    public void executeCommand(CommandSender sender, String[] args) {
        CommandResponseHandler resp = new CommandResponseHandler(this, sender);

        if (!sender.hasPermission("spark")) {
            resp.replyPrefixed(TextComponent.of("You do not have permission to use this command.", TextColor.RED));
            return;
        }

        if (args.length == 0) {
            resp.replyPrefixed(TextComponent.builder("")
                    .append(TextComponent.of("spark", TextColor.WHITE))
                    .append(Component.space())
                    .append(TextComponent.of("v" + getPlugin().getVersion(), TextColor.GRAY))
                    .build()
            );
            resp.replyPrefixed(TextComponent.builder("").color(TextColor.GRAY)
                    .append(TextComponent.of("Use "))
                    .append(TextComponent.builder("/" + getPlugin().getLabel() + " help")
                            .color(TextColor.WHITE)
                            .decoration(TextDecoration.UNDERLINED, true)
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, getPlugin().getLabel() + " help"))
                            .build()
                    )
                    .append(TextComponent.of(" to view usage information."))
                    .build()
            );
            return;
        }

        ArrayList<String> rawArgs = new ArrayList<>(Arrays.asList(args));
        String alias = rawArgs.remove(0).toLowerCase();

        for (Command command : this.commands) {
            if (command.aliases().contains(alias)) {
                try {
                    command.executor().execute(this, sender, resp, new Arguments(rawArgs));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    resp.replyPrefixed(TextComponent.of(e.getMessage(), TextColor.RED));
                }
                return;
            }
        }

        sendUsage(resp);
    }

    public List<String> tabCompleteCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spark")) {
            return Collections.emptyList();
        }

        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        if (args.length <= 1) {
            List<String> mainCommands = this.commands.stream().map(c -> c.aliases().get(0)).collect(Collectors.toList());
            return TabCompleter.create()
                    .at(0, CompletionSupplier.startsWith(mainCommands))
                    .complete(arguments);
        }

        String alias = arguments.remove(0);
        for (Command command : this.commands) {
            if (command.aliases().contains(alias)) {
                return command.tabCompleter().completions(this, sender, arguments);
            }
        }

        return Collections.emptyList();
    }

    private void sendUsage(CommandResponseHandler sender) {
        sender.replyPrefixed(TextComponent.builder("")
                .append(TextComponent.of("spark", TextColor.WHITE))
                .append(Component.space())
                .append(TextComponent.of("v" + getPlugin().getVersion(), TextColor.GRAY))
                .build()
        );
        for (Command command : this.commands) {
            String usage = getPlugin().getLabel() + " " + command.aliases().get(0);
            ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, usage);
            sender.reply(TextComponent.builder("")
                    .append(TextComponent.builder(">").color(TextColor.GOLD).decoration(TextDecoration.BOLD, true).build())
                    .append(Component.space())
                    .append(TextComponent.builder("/" + usage).color(TextColor.GRAY).clickEvent(clickEvent).build())
                    .build()
            );
            for (Command.ArgumentInfo arg : command.arguments()) {
                if (arg.requiresParameter()) {
                    sender.reply(TextComponent.builder("       ")
                            .append(TextComponent.of("[", TextColor.DARK_GRAY))
                            .append(TextComponent.of("--" + arg.argumentName(), TextColor.GRAY))
                            .append(Component.space())
                            .append(TextComponent.of("<" + arg.parameterDescription() + ">", TextColor.DARK_GRAY))
                            .append(TextComponent.of("]", TextColor.DARK_GRAY))
                            .build()
                    );
                } else {
                    sender.reply(TextComponent.builder("       ")
                            .append(TextComponent.of("[", TextColor.DARK_GRAY))
                            .append(TextComponent.of("--" + arg.argumentName(), TextColor.GRAY))
                            .append(TextComponent.of("]", TextColor.DARK_GRAY))
                            .build()
                    );
                }
            }
        }
    }

}
