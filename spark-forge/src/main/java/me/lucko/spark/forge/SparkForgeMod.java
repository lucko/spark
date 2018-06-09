package me.lucko.spark.forge;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
        modid = "spark",
        name = "spark",
        version = "@version@",
        acceptableRemoteVersions = "*"
)
public class SparkForgeMod {

    @EventHandler
    public void init(FMLInitializationEvent e) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            ForgeClientCommandHandler.register();
        }
    }

    @EventHandler
    public void serverInit(FMLServerStartingEvent e) {
        e.registerServerCommand(new ForgeServerCommandHandler());
    }

}
