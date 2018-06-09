package me.lucko.spark.forge;

import me.lucko.spark.profiler.TickCounter;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;

public class ForgeClientCommandHandler extends ForgeCommandHandler {

    public static void register() {
        ClientCommandHandler.instance.registerCommand(new ForgeClientCommandHandler());
    }

    @Override
    protected void broadcast(ITextComponent msg) {
        Minecraft.getMinecraft().player.sendMessage(msg);
    }

    @Override
    protected TickCounter newTickCounter() {
        return new ForgeTickCounter(TickEvent.Type.CLIENT);
    }

    @Override
    public String getLabel() {
        return "sparkclient";
    }

    @Override
    public String getName() {
        return "sparkclient";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("cprofiler");
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
