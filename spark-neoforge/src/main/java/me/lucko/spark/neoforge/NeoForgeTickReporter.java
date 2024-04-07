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

package me.lucko.spark.neoforge;

import me.lucko.spark.common.tick.SimpleTickReporter;
import me.lucko.spark.common.tick.TickReporter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;

public class NeoForgeTickReporter extends SimpleTickReporter implements TickReporter {
    private final TickEvent.Type type;

    public NeoForgeTickReporter(TickEvent.Type type) {
        this.type = type;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e) {
        if (e.type != this.type) {
            return;
        }

        switch (e.phase) {
            case START -> onStart();
            case END -> onEnd();
            default -> throw new AssertionError(e.phase);
        }
    }
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.type != this.type) {
            return;
        }

        switch (e.phase) {
            case START -> onStart();
            case END -> onEnd();
            default -> throw new AssertionError(e.phase);
        }
    }

    @Override
    public void start() {
        NeoForge.EVENT_BUS.register(this);
    }

    @Override
    public void close() {
        NeoForge.EVENT_BUS.unregister(this);
        super.close();
    }

}
