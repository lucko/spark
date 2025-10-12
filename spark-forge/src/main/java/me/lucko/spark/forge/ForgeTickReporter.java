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

import me.lucko.spark.common.tick.SimpleTickReporter;
import me.lucko.spark.common.tick.TickReporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.listener.EventListener;
import org.jspecify.annotations.NonNull;

public abstract class ForgeTickReporter extends SimpleTickReporter implements TickReporter {
    private final EventBus<? extends @NonNull TickEvent> preBus;
    private final EventBus<? extends @NonNull TickEvent> postBus;

    private EventListener preListener;
    private EventListener postListener;

    protected ForgeTickReporter(EventBus<? extends @NonNull TickEvent> preBus, EventBus<? extends @NonNull TickEvent> postBus) {
        this.preBus = preBus;
        this.postBus = postBus;
    }

    public void onStart(TickEvent e) {
        onStart();
    }

    public void onEnd(TickEvent e) {
        onEnd();
    }

    @Override
    public void start() {
        this.preListener = this.preBus.addListener(this::onStart);
        this.postListener = this.postBus.addListener(this::onEnd);
    }

    @Override
    public void close() {
        if (this.preListener != null) {
            this.preBus.removeListener(this.preListener);
            this.preListener = null;
        }
        if (this.postListener != null) {
            this.postBus.removeListener(this.postListener);
            this.postListener = null;
        }
        super.close();
    }

    public static final class Server extends ForgeTickReporter {
        public Server() {
            super(TickEvent.ServerTickEvent.Pre.BUS, TickEvent.ServerTickEvent.Post.BUS);
        }
    }

    public static final class Client extends ForgeTickReporter {
        public Client() {
            super(TickEvent.ClientTickEvent.Pre.BUS, TickEvent.ClientTickEvent.Post.BUS);
        }
    }

}
