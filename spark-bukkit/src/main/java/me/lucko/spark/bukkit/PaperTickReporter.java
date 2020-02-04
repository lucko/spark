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

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import me.lucko.spark.common.sampler.tick.AbstractTickReporter;
import me.lucko.spark.common.sampler.tick.TickReporter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class PaperTickReporter extends AbstractTickReporter implements TickReporter, Listener {
    private final Plugin plugin;

    public PaperTickReporter(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerTickEvent(ServerTickEndEvent e) {
        onTick(e.getTickDuration());
    }

    @Override
    public void start() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
    }

}
