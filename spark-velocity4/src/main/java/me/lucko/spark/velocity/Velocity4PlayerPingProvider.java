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

package me.lucko.spark.velocity;

import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.connection.Player;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;

import java.util.Map;

public class Velocity4PlayerPingProvider implements PlayerPingProvider {
    private final ProxyServer proxy;

    public Velocity4PlayerPingProvider(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public Map<String, Integer> poll() {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        for (Player player : this.proxy.connectedPlayers()) {
            builder.put(player.username(), (int) player.ping());
        }
        return builder.build();
    }
}
