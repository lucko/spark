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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.monitor.tick.TpsCalculator;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.TickCounter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SparkBukkitPlugin extends JavaPlugin implements SparkPlugin<CommandSender> {

    private final SparkPlatform<CommandSender> platform = new SparkPlatform<>(this);

    @Override
    public void onEnable() {
        this.platform.enable();

        // override Spigot's TPS command with our own.
        if (getConfig().getBoolean("override-tps-command", true)) {
            CommandMapUtil.registerCommand(this, (sender, command, label, args) -> {
                if (!sender.hasPermission("spark") && !sender.hasPermission("spark.tps") && !sender.hasPermission("bukkit.command.tps")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                CommandResponseHandler<CommandSender> resp = new CommandResponseHandler<>(this.platform, sender);
                TpsCalculator tpsCalculator = this.platform.getTpsCalculator();
                resp.replyPrefixed("TPS from last 5s, 10s, 1m, 5m, 15m:");
                resp.replyPrefixed(" " + tpsCalculator.toFormattedString());
                return true;
            }, "tps");
        }
    }

    @Override
    public void onDisable() {
        this.platform.disable();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spark")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        this.platform.executeCommand(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("spark")) {
            return Collections.emptyList();
        }
        return this.platform.tabCompleteCommand(sender, args);
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public Path getPluginFolder() {
        return getDataFolder().toPath();
    }

    @Override
    public String getLabel() {
        return "spark";
    }

    @Override
    public Set<CommandSender> getSenders() {
        Set<CommandSender> senders = new HashSet<>(getServer().getOnlinePlayers());
        senders.add(getServer().getConsoleSender());
        return senders;
    }

    @Override
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void sendLink(CommandSender sender, String url) {
        sendMessage(sender, "&7" + url);
    }

    @Override
    public void runAsync(Runnable r) {
        getServer().getScheduler().runTaskAsynchronously(SparkBukkitPlugin.this, r);
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
