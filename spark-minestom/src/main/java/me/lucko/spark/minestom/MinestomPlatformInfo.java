package me.lucko.spark.minestom;

import me.lucko.spark.common.platform.PlatformInfo;
import net.minestom.server.MinecraftServer;

public class MinestomPlatformInfo implements PlatformInfo {
    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public String getName() {
        return "Minestom";
    }

    @Override
    public String getVersion() {
        return MinecraftServer.VERSION_NAME + "-" + MinecraftServer.getBrandName();
    }

    @Override
    public String getMinecraftVersion() {
        return MinecraftServer.VERSION_NAME;
    }
}
