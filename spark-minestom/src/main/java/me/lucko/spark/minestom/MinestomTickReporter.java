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

package me.lucko.spark.minestom;

import me.lucko.spark.common.tick.AbstractTickReporter;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.server.ServerTickMonitorEvent;

import java.util.UUID;

public class MinestomTickReporter extends AbstractTickReporter {
    private final EventNode<Event> node = EventNode.all("sparkTickReporter-" + UUID.randomUUID());

    public MinestomTickReporter() {
        this.node.addListener(ServerTickMonitorEvent.class, event -> onTick(event.getTickMonitor().getTickTime()));
    }

    @Override
    public void start() {
        MinecraftServer.getGlobalEventHandler().addChild(this.node);
    }

    @Override
    public void close() {
        MinecraftServer.getGlobalEventHandler().removeChild(this.node);
    }
}
