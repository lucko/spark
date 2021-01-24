package me.lucko.spark.nukkit;

import cn.nukkit.Server;
import cn.nukkit.network.protocol.ProtocolInfo;
import me.lucko.spark.common.platform.AbstractPlatformInfo;

public class NukkitPlatformInfo extends AbstractPlatformInfo {
    private final Server server;

    public NukkitPlatformInfo(Server server) {
        this.server = server;
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public String getName() {
        return "Nukkit";
    }

    @Override
    public String getVersion() {
        return this.server.getVersion();
    }

    @Override
    public String getMinecraftVersion() {
        return ProtocolInfo.MINECRAFT_VERSION;
    }
}
