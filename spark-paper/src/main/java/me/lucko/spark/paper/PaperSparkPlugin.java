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

package me.lucko.spark.paper;

import java.util.List;
import java.util.stream.Stream;
import me.lucko.spark.bukkit.common.AbstractSparkPlugin;
import me.lucko.spark.bukkit.common.BukkitClassSourceLookup;
import me.lucko.spark.bukkit.common.BukkitServerConfigProvider;
import me.lucko.spark.bukkit.common.PaperTickHook;
import me.lucko.spark.bukkit.common.PaperTickReporter;
import me.lucko.spark.bukkit.common.placeholder.SparkMVdWPlaceholders;
import me.lucko.spark.bukkit.common.placeholder.SparkPlaceholderApi;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class PaperSparkPlugin extends JavaPlugin implements AbstractSparkPlugin {
    private ThreadDumper gameThreadDumper;
    private PaperCommandMapUtil commandMapUtil;

    private SparkPlatform platform;

    private CommandExecutor tpsCommand = null;

    @Override
    public void onEnable() {
        this.gameThreadDumper = new ThreadDumper.Specific(Thread.currentThread());
        this.commandMapUtil = new PaperCommandMapUtil(this.getServer());

        this.platform = new SparkPlatform(this);
        this.platform.enable();

        // override Spigot's TPS command with our own.
        if (this.platform.getConfiguration().getBoolean("overrideTpsCommand", true)) {
            this.tpsCommand = (sender, command, label, args) -> {
                if (!sender.hasPermission("spark") && !sender.hasPermission("spark.tps") && !sender.hasPermission("bukkit.command.tps")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                PaperCommandSender s = new PaperCommandSender(sender) {
                    @Override
                    public boolean hasPermission(String permission) {
                        return true;
                    }
                };
                this.platform.executeCommand(s, new String[]{"tps"});
                return true;
            };
            this.commandMapUtil.registerCommand(this, this.tpsCommand, "tps");
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
        this.platform.disable();
        if (this.tpsCommand != null) {
            this.commandMapUtil.unregisterCommand(this.tpsCommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.platform.executeCommand(new PaperCommandSender(sender), args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.platform.tabCompleteCommand(new PaperCommandSender(sender), args);
    }

    @Override
    public Stream<PaperCommandSender> getCommandSenders() {
        return Stream.concat(
                getServer().getOnlinePlayers().stream(),
                Stream.of(getServer().getConsoleSender())
        ).map(PaperCommandSender::new);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    @Override
    public TickHook createTickHook() {
        return new PaperTickHook(this);
    }

    @Override
    public TickReporter createTickReporter() {
        return new PaperTickReporter(this);
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new BukkitClassSourceLookup();
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new PaperPlayerPingProvider(getServer());
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new BukkitServerConfigProvider();
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new PaperWorldInfoProvider(getServer());
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new PaperPlatformInfo(getServer());
    }
}
