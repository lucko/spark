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

import me.lucko.spark.bukkit.placeholder.SparkMVdWPlaceholders;
import me.lucko.spark.bukkit.placeholder.SparkPlaceholderApi;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.TickCounter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class BukkitSparkPlugin extends JavaPlugin implements SparkPlugin {

    private CommandExecutor tpsCommand = null;
    private SparkPlatform platform;

    @Override
    public void onEnable() {
        this.platform = new SparkPlatform(this);
        this.platform.enable();

        // override Spigot's TPS command with our own.
        if (getConfig().getBoolean("override-tps-command", true)) {
            this.tpsCommand = (sender, command, label, args) -> {
                if (!sender.hasPermission("spark") && !sender.hasPermission("spark.tps") && !sender.hasPermission("bukkit.command.tps")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                BukkitCommandSender s = new BukkitCommandSender(sender) {
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
        this.platform.disable();
        if (this.tpsCommand != null) {
            CommandMapUtil.unregisterCommand(this.tpsCommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.platform.executeCommand(new BukkitCommandSender(sender), args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.platform.tabCompleteCommand(new BukkitCommandSender(sender), args);
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
    public Stream<BukkitCommandSender> getSendersWithPermission(String permission) {
        return Stream.concat(
                getServer().getOnlinePlayers().stream().filter(player -> player.hasPermission(permission)),
                Stream.of(getServer().getConsoleSender())
        ).map(BukkitCommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        getServer().getScheduler().runTaskAsynchronously(BukkitSparkPlugin.this, task);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return new ThreadDumper.Specific(new long[]{Thread.currentThread().getId()});
    }

    @Override
    public TickCounter createTickCounter() {
        return new BukkitTickCounter(this);
    }
}
