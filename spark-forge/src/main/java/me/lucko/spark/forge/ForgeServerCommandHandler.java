package me.lucko.spark.forge;

import me.lucko.spark.profiler.TickCounter;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;

public class ForgeServerCommandHandler extends ForgeCommandHandler {

    @Override
    protected void broadcast(ITextComponent msg) {
        FMLCommonHandler.instance().getMinecraftServerInstance().sendMessage(msg);

        List<EntityPlayerMP> players = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
        for (EntityPlayerMP player : players) {
            if (player.canUseCommand(4, "spark.profiler")) {
                player.sendMessage(msg);
            }
        }
    }

    @Override
    protected TickCounter newTickCounter() {
        return new ForgeTickCounter(TickEvent.Type.SERVER);
    }

    @Override
    public String getLabel() {
        return "spark";
    }

    @Override
    public String getName() {
        return "spark";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("profiler");
    }
}
