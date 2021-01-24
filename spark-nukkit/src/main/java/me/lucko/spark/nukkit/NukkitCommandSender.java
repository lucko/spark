package me.lucko.spark.nukkit;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

public class NukkitCommandSender extends AbstractCommandSender<CommandSender> {

    public NukkitCommandSender(CommandSender delegate) {
        super(delegate);
    }

    @Override
    public String getName() {
        return this.delegate.getName();
    }

    @Override
    public UUID getUniqueId() {
        if (this.delegate instanceof Player) {
            return ((Player) this.delegate).getUniqueId();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        this.delegate.sendMessage(LegacyComponentSerializer.legacySection().serialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.delegate.hasPermission(permission);
    }
}