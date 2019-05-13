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

import me.lucko.spark.common.CommandSender;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.TickCounter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SparkBukkitPlugin extends JavaPlugin implements SparkPlugin {

    private final SparkPlatform platform = new SparkPlatform(this);
    private CommandExecutor tpsCommand = null;

    @Override
    public void onEnable() {
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
    }

    @Override
    public void onDisable() {
        this.platform.disable();
        if (this.tpsCommand != null) {
            CommandMapUtil.unregisterCommand(this.tpsCommand);
        }
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, Command command, String label, String[] args) {
        this.platform.executeCommand(new BukkitCommandSender(sender), args);
        return true;
    }

    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, Command command, String alias, String[] args) {
        return this.platform.tabCompleteCommand(new BukkitCommandSender(sender), args);
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
    public Set<CommandSender> getSendersWithPermission(String permission) {
        List<org.bukkit.command.CommandSender> senders = new LinkedList<>(getServer().getOnlinePlayers());
        senders.removeIf(sender -> !sender.hasPermission(permission));
        senders.add(getServer().getConsoleSender());
        return senders.stream().map(BukkitCommandSender::new).collect(Collectors.toSet());
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
