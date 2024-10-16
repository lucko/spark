package me.lucko.spark.allay;

import me.lucko.spark.common.platform.PlatformInfo;
import org.allaymc.api.AllayAPI;
import org.allaymc.api.network.ProtocolInfo;

/**
 * @author IWareQ
 */
public class AllayPlatformInfo implements PlatformInfo {

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public String getName() {
        return "Allay";
    }

    @Override
    public String getBrand() {
        return "Allay";
    }

    @Override
    public String getVersion() {
        return AllayAPI.API_VERSION;
    }

    @Override
    public String getMinecraftVersion() {
        return ProtocolInfo.getMinecraftVersionStr();
    }
}
