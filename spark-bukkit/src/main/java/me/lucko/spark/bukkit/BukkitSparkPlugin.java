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

package me.lucko.spark.bukkit;

import me.lucko.spark.api.Spark;
import me.lucko.spark.bukkit.placeholder.SparkMVdWPlaceholders;
import me.lucko.spark.bukkit.placeholder.SparkPlaceholderApi;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

public class BukkitSparkPlugin extends JavaPlugin implements SparkPlugin {
    private BukkitAudiences audienceFactory;
    private ThreadDumper gameThreadDumper;

    private SparkPlatform platform;

    private CommandExecutor tpsCommand = null;

    @Override
    public void onEnable() {
        boolean detectedSparkMod = classExists("me.lucko.spark.forge.ForgeSparkMod")
                || classExists("me.lucko.spark.fabric.FabricSparkMod")
                || classExists("me.lucko.spark.neoforge.NeoForgeSparkMod");
        if (detectedSparkMod) {
            getLogger().warning("The spark Bukkit plugin should not be installed when running hybrid Bukkit/modded servers if the spark mod is also installed. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.audienceFactory = BukkitAudiences.create(this);
        this.gameThreadDumper = new ThreadDumper.Specific(Thread.currentThread());

        this.platform = new SparkPlatform(this);
        this.platform.enable();

        // override Spigot's TPS command with our own.
        if (this.platform.getConfiguration().getBoolean("overrideTpsCommand", true)) {
            this.tpsCommand = (sender, command, label, args) -> {
                if (!sender.hasPermission("spark") && !sender.hasPermission("spark.tps") && !sender.hasPermission("bukkit.command.tps")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                BukkitCommandSender s = new BukkitCommandSender(sender, this.audienceFactory) {
                    @Override
                    public boolean hasPermission(String permission) {
                        return true;
                    }
                };
                this.platform.executeCommand(s, new String[]{"tps"});
                return true;
            };
            CommandMapUtil.registerCommand(this, this.tpsCommand, "tps");
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new SparkPlaceholderApi(this, this.platform);
            getLogger().info("Registered PlaceholderAPI placeholders");
        }
        if (getServer().getPluginManager().isPluginEnabled("MVdWPlaceholderAPI")) {
            new SparkMVdWPlaceholders(this, this.platform);
            getLogger().info("Registered MVdWPlaceholderAPI placeholders");
        }
    }

    @Override
    public void onDisable() {
        if (this.platform != null) {
            this.platform.disable();
        }
        if (this.tpsCommand != null) {
            CommandMapUtil.unregisterCommand(this.tpsCommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.platform.executeCommand(new BukkitCommandSender(sender, this.audienceFactory), args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.platform.tabCompleteCommand(new BukkitCommandSender(sender, this.audienceFactory), args);
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return getDataFolder().toPath();
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<BukkitCommandSender> getCommandSenders() {
        return Stream.concat(
                getServer().getOnlinePlayers().stream(),
                Stream.of(getServer().getConsoleSender())
        ).map(sender -> new BukkitCommandSender(sender, this.audienceFactory));
    }

    @Override
    public void executeAsync(Runnable task) {
        getServer().getScheduler().runTaskAsynchronously(this, task);
    }

    @Override
    public void executeSync(Runnable task) {
        getServer().getScheduler().runTask(this, task);
    }

    @Override
    public void log(Level level, String msg) {
        getLogger().log(level, msg);
    }

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        getLogger().log(level, msg, throwable);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    @Override
    public TickHook createTickHook() {
        if (classExists("com.destroystokyo.paper.event.server.ServerTickStartEvent")) {
            getLogger().info("Using Paper ServerTickStartEvent for tick monitoring");
            return new PaperTickHook(this);
        } else {
            getLogger().info("Using Bukkit scheduler for tick monitoring");
            return new BukkitTickHook(this);
        }
    }

    @Override
    public TickReporter createTickReporter() {
        if (classExists("com.destroystokyo.paper.event.server.ServerTickEndEvent")) {
            return new PaperTickReporter(this);
        }
        return null;
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new BukkitClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                Arrays.asList(getServer().getPluginManager().getPlugins()),
                Plugin::getName,
                plugin -> plugin.getDescription().getVersion(),
                plugin -> String.join(", ", plugin.getDescription().getAuthors()),
                plugin -> plugin.getDescription().getDescription()
        );
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        if (BukkitPlayerPingProvider.isSupported()) {
            return new BukkitPlayerPingProvider(getServer());
        } else {
            return null;
        }
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new BukkitServerConfigProvider();
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new BukkitWorldInfoProvider(getServer());
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new BukkitPlatformInfo(getServer());
    }

    @Override
    public void registerApi(Spark api) {
        getServer().getServicesManager().register(Spark.class, api, this, ServicePriority.Normal);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
