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
import me.lucko.spark.sampler.ThreadDumper;
import me.lucko.spark.sampler.TickCounter;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class SparkBukkitPlugin extends JavaPlugin {

    private final SparkPlatform<CommandSender> sparkPlatform = new SparkPlatform<CommandSender>() {

        private String colorize(String message) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        private void broadcast(String msg) {
            getServer().getConsoleSender().sendMessage(msg);
            for (Player player : getServer().getOnlinePlayers()) {
                if (player.hasPermission("spark")) {
                    player.sendMessage(msg);
                }
            }
        }

        @Override
        public String getVersion() {
            return SparkBukkitPlugin.this.getDescription().getVersion();
        }

        @Override
        public String getLabel() {
            return "spark";
        }

        @Override
        public void sendMessage(CommandSender sender, String message) {
            sender.sendMessage(colorize(message));
        }

        @Override
        public void sendMessage(String message) {
            String msg = colorize(message);
            broadcast(msg);
        }

        @Override
        public void sendLink(String url) {
            String msg = colorize("&7" + url);
            broadcast(msg);
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
        public TickCounter newTickCounter() {
            return new BukkitTickCounter(SparkBukkitPlugin.this);
        }
    };

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spark")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        this.sparkPlatform.executeCommand(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("spark")) {
            return Collections.emptyList();
        }
        return this.sparkPlatform.tabCompleteCommand(sender, args);
    }
}
