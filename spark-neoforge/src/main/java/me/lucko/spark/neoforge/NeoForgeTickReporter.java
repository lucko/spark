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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public abstract class NeoForgeTickReporter extends SimpleTickReporter implements TickReporter {

    @Override
    public void start() {
        NeoForge.EVENT_BUS.register(this);
    }

    @Override
    public void close() {
        NeoForge.EVENT_BUS.unregister(this);
        super.close();
    }

    public static final class Server extends NeoForgeTickReporter {

        @SubscribeEvent
        public void onTickStart(ServerTickEvent.Pre e) {
            onStart();
        }

        @SubscribeEvent
        public void onTickEnd(ServerTickEvent.Post e) {
            onEnd();
        }
    }

    public static final class Client extends NeoForgeTickReporter {

        @SubscribeEvent
        public void onTickStart(ClientTickEvent.Pre e) {
            onStart();
        }

        @SubscribeEvent
        public void onTickEnd(ClientTickEvent.Post e) {
            onEnd();
        }
    }

}
