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
import me.lucko.bytesocks.client.BytesocksClient;
import me.lucko.spark.common.activitylog.ActivityLog;
import me.lucko.spark.common.api.SparkApi;
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
import me.lucko.spark.common.monitor.net.NetworkMonitor;
import me.lucko.spark.common.monitor.ping.PingStatistics;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.monitor.tick.SparkTickStatistics;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.PlatformStatisticsProvider;
import me.lucko.spark.common.sampler.BackgroundSamplerManager;
import me.lucko.spark.common.sampler.SamplerContainer;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.common.util.BytebinClient;
import me.lucko.spark.common.util.TemporaryFiles;
import me.lucko.spark.common.util.classfinder.ClassFinder;
import me.lucko.spark.common.util.config.Configuration;
import me.lucko.spark.common.util.config.FileConfiguration;
import me.lucko.spark.common.util.config.RuntimeConfiguration;
import me.lucko.spark.common.util.log.SparkStaticLogger;
import me.lucko.spark.common.ws.TrustedKeyStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

/**
 * Abstract spark implementation used by all platforms.
 */
public class SparkPlatform {

    /** The date time formatter instance used by the platform */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private final SparkPlugin plugin;
    private final TemporaryFiles temporaryFiles;
    private final Configuration configuration;
    private final String viewerUrl;
    private final BytebinClient bytebinClient;
    private final BytesocksClient bytesocksClient;
    private final TrustedKeyStore trustedKeyStore;
    private final boolean disableResponseBroadcast;
    private final List<CommandModule> commandModules;
    private final List<Command> commands;
    private final ReentrantLock commandExecuteLock = new ReentrantLock(true);
    private final ActivityLog activityLog;
    private final SamplerContainer samplerContainer;
    private final BackgroundSamplerManager backgroundSamplerManager;
    private final TickHook tickHook;
    private final TickReporter tickReporter;
    private final TickStatistics tickStatistics;
    private final PingStatistics pingStatistics;
    private final PlatformStatisticsProvider statisticsProvider;
    private Map<String, GarbageCollectorStatistics> startupGcStatistics = ImmutableMap.of();
    private long serverNormalOperationStartTime;
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public SparkPlatform(SparkPlugin plugin) {
        this.plugin = plugin;
        SparkStaticLogger.setLogger(plugin);

        this.temporaryFiles = new TemporaryFiles(this.plugin.getPlatformInfo().getType() == PlatformInfo.Type.CLIENT
                ? this.plugin.getPluginDirectory().resolve("tmp-client")
                : this.plugin.getPluginDirectory().resolve("tmp")
        );
        this.configuration = Configuration.combining(
                RuntimeConfiguration.SYSTEM_PROPERTIES,
                RuntimeConfiguration.ENVIRONMENT_VARIABLES,
                new FileConfiguration(this.plugin.getPluginDirectory().resolve("config.json"))
        );

        this.viewerUrl = this.configuration.getString("viewerUrl", "https://spark.lucko.me/");
        String bytebinUrl = this.configuration.getString("bytebinUrl", "https://spark-usercontent.lucko.me/");
        String bytesocksHost = this.configuration.getString("bytesocksHost", "spark-usersockets.lucko.me");

        this.bytebinClient = new BytebinClient(bytebinUrl, "spark-plugin");
        this.bytesocksClient = BytesocksClient.create(bytesocksHost, "spark-plugin");
        this.trustedKeyStore = new TrustedKeyStore(this.configuration);

        this.disableResponseBroadcast = this.configuration.getBoolean("disableResponseBroadcast", false);

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

        this.samplerContainer = new SamplerContainer();
        this.backgroundSamplerManager = new BackgroundSamplerManager(this, this.configuration);

        TickStatistics tickStatistics = plugin.createTickStatistics();
        this.tickHook = plugin.createTickHook();
        this.tickReporter = plugin.createTickReporter();
        if (tickStatistics == null && (this.tickHook != null || this.tickReporter != null)) {
            tickStatistics = new SparkTickStatistics();
        }
        this.tickStatistics = tickStatistics;

        PlayerPingProvider pingProvider = plugin.createPlayerPingProvider();
        this.pingStatistics = pingProvider != null ? new PingStatistics(pingProvider) : null;

        this.statisticsProvider = new PlatformStatisticsProvider(this);
    }

    public void enable() {
        if (!this.enabled.compareAndSet(false, true)) {
            throw new RuntimeException("Platform has already been enabled!");
        }

        if (this.tickHook != null && this.tickStatistics instanceof SparkTickStatistics) {
            this.tickHook.addCallback((TickHook.Callback) this.tickStatistics);
            this.tickHook.start();
        }
        if (this.tickReporter != null&& this.tickStatistics instanceof SparkTickStatistics) {
            this.tickReporter.addCallback((TickReporter.Callback) this.tickStatistics);
            this.tickReporter.start();
        }
        if (this.pingStatistics != null) {
            this.pingStatistics.start();
        }
        CpuMonitor.ensureMonitoring();
        NetworkMonitor.ensureMonitoring();

        // poll startup GC statistics after plugins & the world have loaded
        this.plugin.executeAsync(() -> {
            this.startupGcStatistics = GarbageCollectorStatistics.pollStats();
            this.serverNormalOperationStartTime = System.currentTimeMillis();
        });

        SparkApi api = new SparkApi(this);
        this.plugin.registerApi(api);
        SparkApi.register(api);

        this.backgroundSamplerManager.initialise();
    }

    public void disable() {
        if (this.tickHook != null) {
            this.tickHook.close();
        }
        if (this.tickReporter != null) {
            this.tickReporter.close();
        }
        if (this.pingStatistics != null) {
            this.pingStatistics.close();
        }

        for (CommandModule module : this.commandModules) {
            module.close();
        }

        this.samplerContainer.close();

        SparkApi.unregister();

        this.temporaryFiles.deleteTemporaryFiles();
    }

    public SparkPlugin getPlugin() {
        return this.plugin;
    }

    public TemporaryFiles getTemporaryFiles() {
        return this.temporaryFiles;
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public String getViewerUrl() {
        return this.viewerUrl;
    }

    public BytebinClient getBytebinClient() {
        return this.bytebinClient;
    }

    public BytesocksClient getBytesocksClient() {
        return this.bytesocksClient;
    }

    public TrustedKeyStore getTrustedKeyStore() {
        return this.trustedKeyStore;
    }

    public boolean shouldBroadcastResponse() {
        return !this.disableResponseBroadcast;
    }

    public List<Command> getCommands() {
        return this.commands;
    }

    public ActivityLog getActivityLog() {
        return this.activityLog;
    }

    public SamplerContainer getSamplerContainer() {
        return this.samplerContainer;
    }

    public BackgroundSamplerManager getBackgroundSamplerManager() {
        return this.backgroundSamplerManager;
    }

    public TickHook getTickHook() {
        return this.tickHook;
    }

    public TickReporter getTickReporter() {
        return this.tickReporter;
    }

    public PlatformStatisticsProvider getStatisticsProvider() {
        return this.statisticsProvider;
    }

    public ClassSourceLookup createClassSourceLookup() {
        return this.plugin.createClassSourceLookup();
    }

    public ClassFinder createClassFinder() {
        return this.plugin.createClassFinder();
    }

    public TickStatistics getTickStatistics() {
        return this.tickStatistics;
    }

    public PingStatistics getPingStatistics() {
        return this.pingStatistics;
    }

    public Map<String, GarbageCollectorStatistics> getStartupGcStatistics() {
        return this.startupGcStatistics;
    }

    public long getServerNormalOperationStartTime() {
        return this.serverNormalOperationStartTime;
    }

    public boolean hasEnabled() {
        return this.enabled.get();
    }

    public Path resolveSaveFile(String prefix, String extension) {
        Path pluginFolder = this.plugin.getPluginDirectory();
        try {
            Files.createDirectories(pluginFolder);
        } catch (IOException e) {
            // ignore
        }

        return pluginFolder.resolve(prefix + "-" + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + "." + extension);
    }

    private List<Command> getAvailableCommands(CommandSender sender) {
        if (sender.hasPermission("spark")) {
            return this.commands;
        }
        return this.commands.stream()
                .filter(c -> sender.hasPermission("spark." + c.primaryAlias()))
                .collect(Collectors.toList());
    }

    public Set<String> getAllSparkPermissions() {
        return Stream.concat(
                Stream.of("spark"),
                this.commands.stream()
                        .map(Command::primaryAlias)
                        .map(alias -> "spark." + alias)
        ).collect(Collectors.toSet());
    }

    public boolean hasPermissionForAnyCommand(CommandSender sender) {
        return !getAvailableCommands(sender).isEmpty();
    }

    public CompletableFuture<Void> executeCommand(CommandSender sender, String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicReference<Thread> executorThread = new AtomicReference<>();
        AtomicReference<Thread> timeoutThread = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        // execute the command
        this.plugin.executeAsync(() -> {
            executorThread.set(Thread.currentThread());
            this.commandExecuteLock.lock();
            try {
                executeCommand0(sender, args);
                future.complete(null);
            } catch (Throwable e) {
                this.plugin.log(Level.SEVERE, "Exception occurred whilst executing a spark command", e);
                future.completeExceptionally(e);
            } finally {
                this.commandExecuteLock.unlock();
                executorThread.set(null);
                completed.set(true);

                Thread timeout = timeoutThread.get();
                if (timeout != null) {
                    timeout.interrupt();
                }
            }
        });

        // schedule a task to detect timeouts
        this.plugin.executeAsync(() -> {
            timeoutThread.set(Thread.currentThread());
            int warningIntervalSeconds = 5;

            try {
                if (completed.get()) {
                    return;
                }
                
                for (int i = 1; i <= 3; i++) {
                    try {
                        Thread.sleep(warningIntervalSeconds * 1000);
                    } catch (InterruptedException e) {
                        // ignore
                    }

                    if (completed.get()) {
                        return;
                    }

                    Thread executor = executorThread.get();
                    if (executor == null) {
                        getPlugin().log(Level.WARNING, "A command execution has not completed after " +
                                (i * warningIntervalSeconds) + " seconds but there is no executor present. Perhaps the executor shutdown?");
                        getPlugin().log(Level.WARNING, "If the command subsequently completes without any errors, this warning should be ignored. :)");

                    } else {
                        String stackTrace = Arrays.stream(executor.getStackTrace())
                                .map(el -> "  " + el.toString())
                                .collect(Collectors.joining("\n"));

                        getPlugin().log(Level.WARNING, "A command execution has not completed after " +
                                (i * warningIntervalSeconds) + " seconds, it *might* be stuck. Trace: \n" + stackTrace);
                        getPlugin().log(Level.WARNING, "If the command subsequently completes without any errors, this warning should be ignored. :)");
                    }
                }
            } finally {
                timeoutThread.set(null);
            }
        });

        return future;
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

            String helpCmd = "/" + getPlugin().getCommandName() + " help";
            resp.replyPrefixed(text()
                    .color(GRAY)
                    .append(text("Run "))
                    .append(text()
                            .content(helpCmd)
                            .color(WHITE)
                            .clickEvent(ClickEvent.runCommand(helpCmd))
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
                    command.executor().execute(this, sender, resp, new Arguments(rawArgs, command.allowSubCommand()));
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

            if (command.allowSubCommand()) {
                Map<String, List<Command.ArgumentInfo>> argumentsBySubCommand = command.arguments().stream()
                        .collect(Collectors.groupingBy(Command.ArgumentInfo::subCommandName, LinkedHashMap::new, Collectors.toList()));

                argumentsBySubCommand.forEach((subCommand, arguments) -> {
                    String subCommandUsage = usage + " " + subCommand;

                    sender.reply(text()
                            .append(text(">", GOLD, BOLD))
                            .append(space())
                            .append(text().content(subCommandUsage).color(GRAY).clickEvent(ClickEvent.suggestCommand(subCommandUsage)).build())
                            .build()
                    );

                    for (Command.ArgumentInfo arg : arguments) {
                        if (arg.argumentName().isEmpty()) {
                            continue;
                        }
                        sender.reply(arg.toComponent("      "));
                    }
                });
            } else {
                sender.reply(text()
                        .append(text(">", GOLD, BOLD))
                        .append(space())
                        .append(text().content(usage).color(GRAY).clickEvent(ClickEvent.suggestCommand(usage)).build())
                        .build()
                );

                for (Command.ArgumentInfo arg : command.arguments()) {
                    sender.reply(arg.toComponent("    "));
                }
            }
        }

        sender.reply(Component.empty());
        sender.replyPrefixed(text()
                .append(text("For full usage information, please go to: "))
                .append(text()
                        .content("https://spark.lucko.me/docs/Command-Usage")
                        .color(WHITE)
                        .clickEvent(ClickEvent.openUrl("https://spark.lucko.me/docs/Command-Usage"))
                        .build()
                )
                .build()
        );
    }

}
