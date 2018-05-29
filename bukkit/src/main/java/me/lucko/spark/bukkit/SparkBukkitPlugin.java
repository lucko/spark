package me.lucko.spark.bukkit;

import me.lucko.spark.common.CommandHandler;
import me.lucko.spark.profiler.ThreadDumper;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class SparkBukkitPlugin extends JavaPlugin {

    private final CommandHandler<CommandSender> commandHandler = new CommandHandler<CommandSender>() {
        @Override
        protected void sendMessage(CommandSender sender, String message) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }

        @Override
        protected void sendLink(CommandSender sender, String url) {
            sendMessage(sender, "&7" + url);
        }

        @Override
        protected void runAsync(Runnable r) {
            getServer().getScheduler().runTaskAsynchronously(SparkBukkitPlugin.this, r);
        }

        @Override
        protected ThreadDumper getDefaultThreadDumper() {
            return new ThreadDumper.Specific(new long[]{Thread.currentThread().getId()});
        }
    };

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spark.profiler")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        this.commandHandler.handleCommand(sender, args);
        return true;
    }
}
