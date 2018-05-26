package me.lucko.spark.bungeecord;

import me.lucko.spark.common.CommandHandler;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class SparkBungeeCordPlugin extends Plugin {

    private final CommandHandler<CommandSender> commandHandler = new CommandHandler<CommandSender>() {
        @Override
        protected void sendMessage(CommandSender sender, String message) {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
        }

        @Override
        protected void runAsync(Runnable r) {
            getProxy().getScheduler().runAsync(SparkBungeeCordPlugin.this, r);
        }
    };

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new Command("profiler", null) {
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (!sender.hasPermission("spark.profiler")) {
                    TextComponent msg = new TextComponent("You do not have permission to use this command.");
                    msg.setColor(ChatColor.RED);
                    sender.sendMessage(msg);
                    return;
                }

                SparkBungeeCordPlugin.this.commandHandler.handleCommand(sender, args);
            }
        });
    }
}
