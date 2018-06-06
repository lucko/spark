package me.lucko.spark.bukkit;

import me.lucko.spark.common.CommandHandler;
import me.lucko.spark.profiler.ThreadDumper;
import me.lucko.spark.profiler.TickCounter;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SparkBukkitPlugin extends JavaPlugin {

    private final CommandHandler<CommandSender> commandHandler = new CommandHandler<CommandSender>() {

        private String colorize(String message) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        private void broadcast(String msg) {
            getServer().getConsoleSender().sendMessage(msg);
            for (Player player : getServer().getOnlinePlayers()) {
                if (player.hasPermission("spark.profiler")) {
                    player.sendMessage(msg);
                }
            }
        }

        @Override
        protected void sendMessage(CommandSender sender, String message) {
            sender.sendMessage(colorize(message));
        }

        @Override
        protected void sendMessage(String message) {
            String msg = colorize(message);
            broadcast(msg);
        }

        @Override
        protected void sendLink(String url) {
            String msg = colorize("&7" + url);
            broadcast(msg);
        }

        @Override
        protected void runAsync(Runnable r) {
            getServer().getScheduler().runTaskAsynchronously(SparkBukkitPlugin.this, r);
        }

        @Override
        protected ThreadDumper getDefaultThreadDumper() {
            return new ThreadDumper.Specific(new long[]{Thread.currentThread().getId()});
        }

        @Override
        protected TickCounter newTickCounter() {
            return new BukkitTickCounter(SparkBukkitPlugin.this);
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
