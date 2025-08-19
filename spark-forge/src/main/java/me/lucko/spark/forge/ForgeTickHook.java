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

package me.lucko.spark.forge;

import me.lucko.spark.common.tick.AbstractTickHook;
import me.lucko.spark.common.tick.TickHook;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.listener.EventListener;

import java.util.Objects;

public class ForgeTickHook extends AbstractTickHook implements TickHook {
    private final EventBus<? extends TickEvent> bus;
    private EventListener listener;

    public ForgeTickHook(TickEvent.Type type) {
        this.bus = switch (type) {
            case CLIENT -> TickEvent.ClientTickEvent.Pre.BUS;
            case SERVER -> TickEvent.ServerTickEvent.Pre.BUS;
            default -> null;
        };
        Objects.requireNonNull(this.bus, "bus");
    }

    public void onTick(TickEvent e) {
        onTick();
    }

    @Override
    public void start() {
        this.listener = this.bus.addListener(this::onTick);
    }

    @Override
    public void close() {
        if (this.listener != null) {
            this.bus.removeListener(this.listener);
            this.listener = null;
        }
    }

}
