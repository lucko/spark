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
import com.google.common.collect.ImmutableMap;

import me.lucko.spark.common.activitylog.ActivityLog;
import me.lucko.spark.common.command.Arguments;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.command.modules.ActivityLogModule;
import me.lucko.spark.common.command.modules.GcMonitoringModule;
import me.lucko.spark.common.command.modules.HealthModule;
import me.lucko.spark.common.command.modules.HeapAnalysisModule;
import me.lucko.spark.common.command.modules.SamplerModule;
import me.lucko.spark.common.command.modules.TickMonitoringModule;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.command.tabcomplete.CompletionSupplier;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.monitor.cpu.CpuMonitor;
import me.lucko.spark.common.monitor.memory.GarbageCollectorStatistics;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.common.util.BytebinClient;

import net.kyori.adventure.text.event.ClickEvent;

import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.UNDERLINED;

/**
 * Abstract spark implementation used by all platforms.
 */
public class SparkPlatform {

    /** The URL of the viewer frontend */
    public static final String VIEWER_URL = "https://spark.lucko.me/";
    /** The shared okhttp client */
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();
    /** The bytebin instance used by the platform */
    public static final BytebinClient BYTEBIN_CLIENT = new BytebinClient(OK_HTTP_CLIENT, "https://bytebin.lucko.me/", "spark-plugin");

    private final SparkPlugin plugin;
    private final List<CommandModule> commandModules;
    private final List<Command> commands;
    private final ReentrantLock commandExecuteLock = new ReentrantLock(true);
    private final ActivityLog activityLog;
    private final TickHook tickHook;
    private final TickReporter tickReporter;
    private final TickStatistics tickStatistics;
    private Map<String, GarbageCollectorStatistics> startupGcStatistics = ImmutableMap.of();
    private long serverNormalOperationStartTime;

    public SparkPlatform(SparkPlugin plugin) {
        this.plugin = plugin;

        this.commandModules = ImmutableList.of(
                new SamplerModule(),
                new HealthModule(),
                new TickMonitoringModule(),
                new GcMonitoringModule(),
                new HeapAnalysisModule(),
                new ActivityLogModule()
        );

        ImmutableList.Builder<Command> commandsBuilder = ImmutableList.builder();
        for (CommandModule module : this.commandModules) {
            module.registerCommands(commandsBuilder::add);
        }
        this.commands = commandsBuilder.build();

        this.activityLog = new ActivityLog(plugin.getPluginDirectory().resolve("activity.json"));
        this.activityLog.load();

        this.tickHook = plugin.createTickHook();
        this.tickReporter = plugin.createTickReporter();
        this.tickStatistics = this.tickHook != null ? new TickStatistics() : null;
    }

    public void enable() {
        if (this.tickHook != null) {
            this.tickHook.addCallback(this.tickStatistics);
            this.tickHook.start();
        }
        if (this.tickReporter != null) {
            this.tickReporter.addCallback(this.tickStatistics);
            this.tickReporter.start();
        }
        CpuMonitor.ensureMonitoring();

        // poll startup GC statistics after plugins & the world have loaded
        this.plugin.executeAsync(() -> {
            this.startupGcStatistics = GarbageCollectorStatistics.pollStats();
            this.serverNormalOperationStartTime = System.currentTimeMillis();
        });
    }

    public void disable() {
        if (this.tickHook != null) {
            this.tickHook.close();
        }
        if (this.tickReporter != null) {
            this.tickReporter.close();
        }

        for (CommandModule module : this.commandModules) {
            module.close();
        }
    }

    public SparkPlugin getPlugin() {
        return this.plugin;
    }

    public ActivityLog getActivityLog() {
        return this.activityLog;
    }

    public TickHook getTickHook() {
        return this.tickHook;
    }

    public TickReporter getTickReporter() {
        return this.tickReporter;
    }

    public TickStatistics getTickStatistics() {
        return this.tickStatistics;
    }

    public Map<String, GarbageCollectorStatistics> getStartupGcStatistics() {
        return this.startupGcStatistics;
    }

    public long getServerNormalOperationStartTime() {
        return this.serverNormalOperationStartTime;
    }

    private List<Command> getAvailableCommands(CommandSender sender) {
        if (sender.hasPermission("spark")) {
            return this.commands;
        }
        return this.commands.stream()
                .filter(c -> sender.hasPermission("spark." + c.primaryAlias()))
                .collect(Collectors.toList());
    }

    public boolean hasPermissionForAnyCommand(CommandSender sender) {
        return !getAvailableCommands(sender).isEmpty();
    }

    public void executeCommand(CommandSender sender, String[] args) {
        this.plugin.executeAsync(() -> {
            this.commandExecuteLock.lock();
            try {
                executeCommand0(sender, args);
            } finally {
                this.commandExecuteLock.unlock();
            }
        });
    }

    private void executeCommand0(CommandSender sender, String[] args) {
        CommandResponseHandler resp = new CommandResponseHandler(this, sender);
        List<Command> commands = getAvailableCommands(sender);

        if (commands.isEmpty()) {
            resp.replyPrefixed(text("You do not have permission to use this command.", RED));
            return;
        }

        if (args.length == 0) {
            resp.replyPrefixed(text()
                    .append(text("spark", WHITE))
                    .append(space())
                    .append(text("v" + getPlugin().getVersion(), GRAY))
                    .build()
            );
            resp.replyPrefixed(text()
                    .color(GRAY)
                    .append(text("Use "))
                    .append(text()
                            .content("/" + getPlugin().getCommandName() + " help")
                            .color(WHITE)
                            .decoration(UNDERLINED, true)
                            .clickEvent(ClickEvent.runCommand("/" + getPlugin().getCommandName() + " help"))
                            .build()
                    )
                    .append(text(" to view usage information."))
                    .build()
            );
            return;
        }

        ArrayList<String> rawArgs = new ArrayList<>(Arrays.asList(args));
        String alias = rawArgs.remove(0).toLowerCase();

        for (Command command : commands) {
            if (command.aliases().contains(alias)) {
                resp.setCommandPrimaryAlias(command.primaryAlias());
                try {
                    command.executor().execute(this, sender, resp, new Arguments(rawArgs));
                } catch (Arguments.ParseException e) {
                    resp.replyPrefixed(text(e.getMessage(), RED));
                }
                return;
            }
        }

        sendUsage(commands, resp);
    }

    public List<String> tabCompleteCommand(CommandSender sender, String[] args) {
        List<Command> commands = getAvailableCommands(sender);
        if (commands.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        if (args.length <= 1) {
            List<String> mainCommands = commands.stream()
                    .map(Command::primaryAlias)
                    .collect(Collectors.toList());

            return TabCompleter.create()
                    .at(0, CompletionSupplier.startsWith(mainCommands))
                    .complete(arguments);
        }

        String alias = arguments.remove(0);
        for (Command command : commands) {
            if (command.aliases().contains(alias)) {
                return command.tabCompleter().completions(this, sender, arguments);
            }
        }

        return Collections.emptyList();
    }

    private void sendUsage(List<Command> commands, CommandResponseHandler sender) {
        sender.replyPrefixed(text()
                .append(text("spark", WHITE))
                .append(space())
                .append(text("v" + getPlugin().getVersion(), GRAY))
                .build()
        );
        for (Command command : commands) {
            String usage = "/" + getPlugin().getCommandName() + " " + command.primaryAlias();
            ClickEvent clickEvent = ClickEvent.suggestCommand(usage);
            sender.reply(text()
                    .append(text(">", GOLD, BOLD))
                    .append(space())
                    .append(text().content(usage).color(GRAY).clickEvent(clickEvent).build())
                    .build()
            );
            for (Command.ArgumentInfo arg : command.arguments()) {
                if (arg.requiresParameter()) {
                    sender.reply(text()
                            .content("       ")
                            .append(text("[", DARK_GRAY))
                            .append(text("--" + arg.argumentName(), GRAY))
                            .append(space())
                            .append(text("<" + arg.parameterDescription() + ">", DARK_GRAY))
                            .append(text("]", DARK_GRAY))
                            .build()
                    );
                } else {
                    sender.reply(text()
                            .content("       ")
                            .append(text("[", DARK_GRAY))
                            .append(text("--" + arg.argumentName(), GRAY))
                            .append(text("]", DARK_GRAY))
                            .build()
                    );
                }
            }
        }
    }

}
