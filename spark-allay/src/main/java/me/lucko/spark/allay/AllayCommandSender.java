package me.lucko.spark.allay;

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.entity.interfaces.EntityPlayer;

import java.util.UUID;

/**
 * @author IWareQ
 */
public class AllayCommandSender extends AbstractCommandSender<CommandSender> {

    public AllayCommandSender(CommandSender delegate) {
        super(delegate);
    }

    @Override
    public String getName() {
        return this.delegate.getCommandSenderName();
    }

    @Override
    public UUID getUniqueId() {
        if (this.delegate instanceof EntityPlayer player)
            return player.getUUID();
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        this.delegate.sendText(LegacyComponentSerializer.legacySection().serialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.delegate.hasPerm(permission);
    }
}
