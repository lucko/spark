package me.lucko.spark.bungeecord;

import me.lucko.spark.common.CommandHandler;
import me.lucko.spark.profiler.ThreadDumper;
import me.lucko.spark.profiler.TickCounter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class SparkBungeeCordPlugin extends Plugin {

    private final CommandHandler<CommandSender> commandHandler = new CommandHandler<CommandSender>() {
        private BaseComponent[] colorize(String message) {
            return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message));
        }

        private void broadcast(BaseComponent... msg) {
            getProxy().getConsole().sendMessage(msg);
            for (ProxiedPlayer player : getProxy().getPlayers()) {
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
            broadcast(colorize(message));
        }

        @Override
        protected void sendLink(String url) {
            TextComponent component = new TextComponent(url);
            component.setColor(ChatColor.GRAY);
            component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            broadcast(component);
        }

        @Override
        protected void runAsync(Runnable r) {
            getProxy().getScheduler().runAsync(SparkBungeeCordPlugin.this, r);
        }

        @Override
        protected ThreadDumper getDefaultThreadDumper() {
            return new ThreadDumper.All();
        }

        @Override
        protected TickCounter newTickCounter() {
            throw new UnsupportedOperationException();
        }
    };

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new Command("sparkbungee", null, "gprofiler") {
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
