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

package me.lucko.spark.bukkit;

import com.google.common.collect.ImmutableMap;

import me.lucko.spark.common.monitor.ping.PlayerPingProvider;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Map;

public class BukkitPlayerPingProvider implements PlayerPingProvider {

    public static boolean isSupported() {
        try {
            Player.Spigot.class.getMethod("getPing");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private final Server server;

    public BukkitPlayerPingProvider(Server server) {
        this.server = server;
    }

    @Override
    public Map<String, Integer> poll() {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        for (Player player : this.server.getOnlinePlayers()) {
            builder.put(player.getName(), player.spigot().getPing());
        }
        return builder.build();
    }
}
