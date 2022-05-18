package me.lucko.spark.minestom;

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;

import java.util.UUID;

public class MinestomCommandSender extends AbstractCommandSender<CommandSender> {
    public MinestomCommandSender(CommandSender delegate) {
        super(delegate);
    }

    @Override
    public String getName() {
        if (delegate instanceof Player player) {
            return player.getUsername();
        } else if (delegate instanceof ConsoleSender) {
            return "Console";
         }else {
            return "unknown:" + delegate.getClass().getSimpleName();
        }
    }

    @Override
    public UUID getUniqueId() {
        if (super.delegate instanceof Player player) {
            return player.getUuid();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        delegate.sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return delegate.hasPermission(permission);
    }
}
