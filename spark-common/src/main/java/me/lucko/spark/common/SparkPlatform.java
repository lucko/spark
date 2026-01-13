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

import com.google.common.collect.ImmutableMap;
import me.lucko.bytesocks.client.BytesocksClient;
import me.lucko.spark.common.activitylog.ActivityLog;
import me.lucko.spark.common.api.SparkApi;
import me.lucko.spark.common.command.CommandManager;
import me.lucko.spark.common.command.sender.CommandSender;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final ActivityLog activityLog;
    private final SamplerContainer samplerContainer;
    private final BackgroundSamplerManager backgroundSamplerManager;
    private final TickHook tickHook;
    private final TickReporter tickReporter;
    private final TickStatistics tickStatistics;
    private final PingStatistics pingStatistics;
    private final PlatformStatisticsProvider statisticsProvider;
    private final CommandManager commandManager;
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private Map<String, GarbageCollectorStatistics> startupGcStatistics = ImmutableMap.of();
    private long serverNormalOperationStartTime;

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

        this.commandManager = new CommandManager(this, this.configuration);
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
        this.commandManager.close();

        if (this.tickHook != null) {
            this.tickHook.close();
        }
        if (this.tickReporter != null) {
            this.tickReporter.close();
        }
        if (this.pingStatistics != null) {
            this.pingStatistics.close();
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

    public CommandManager getCommandManager() {
        return this.commandManager;
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

    public boolean hasPermissionForAnyCommand(CommandSender sender) {
        return this.commandManager.hasPermissionForAnyCommand(sender);
    }

    public CompletableFuture<Void> executeCommand(CommandSender sender, String[] args) {
        return this.commandManager.executeCommand(sender, args);
    }

    public List<String> tabCompleteCommand(CommandSender sender, String[] args) {
        return this.commandManager.tabCompleteCommand(sender, args);
    }

}
