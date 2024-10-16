package me.lucko.spark.allay;

import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import org.allaymc.api.server.Server;

import java.util.HashMap;
import java.util.Map;

/**
 * @author IWareQ
 */
public class AllayPlayerPingProvider implements PlayerPingProvider {

    private final Server server;

    public AllayPlayerPingProvider(Server server) {
        this.server = server;
    }

    @Override
    public Map<String, Integer> poll() {
        Map<String, Integer> result = new HashMap<>();
        for (var player : this.server.getOnlinePlayers().values()) {
            result.put(player.getCommandSenderName(), player.getPing());
        }
        return result;
    }
}
