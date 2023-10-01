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

package me.lucko.spark.geyser;

import com.google.common.collect.ImmutableMap;

import me.lucko.spark.common.monitor.ping.PlayerPingProvider;

import org.cloudburstmc.netty.channel.raknet.RakChildChannel;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Map;

public class GeyserPlayerPingProvider implements PlayerPingProvider {
    private final GeyserApi server;

    public GeyserPlayerPingProvider(GeyserApi server) {
        this.server = server;
    }

    @Override
    public Map<String, Integer> poll() {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        for (GeyserConnection player : this.server.onlineConnections()) {
            if (player.isConsole()) continue;
            RakSessionCodec rakSessionCodec = ((RakChildChannel) ((GeyserSession) player).getUpstream().getSession().getPeer().getChannel()).rakPipeline().get(RakSessionCodec.class);
            builder.put(player.name(), (int) rakSessionCodec.getPing());
        }
        return builder.build();
    }
}
