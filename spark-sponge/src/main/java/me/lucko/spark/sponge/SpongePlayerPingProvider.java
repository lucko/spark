/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.sponge;

import com.google.common.collect.ImmutableMap;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.network.EngineConnectionState;
import org.spongepowered.api.network.ServerConnectionState;
import org.spongepowered.api.network.ServerSideConnection;

import java.util.Map;

public class SpongePlayerPingProvider implements PlayerPingProvider {
    private final Server server;

    public SpongePlayerPingProvider(Server server) {
        this.server = server;
    }

    @Override
    public Map<String, Integer> poll() {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        for (ServerPlayer player : this.server.onlinePlayers()) {
            ServerSideConnection connection = player.connection();
            EngineConnectionState connectionState = connection.state().orElse(null);
            if (connectionState instanceof ServerConnectionState.Game) {
                int latency = ((ServerConnectionState.Game) connectionState).latency();
                builder.put(player.name(), latency);
            }
        }
        return builder.build();
    }
}
