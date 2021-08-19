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

package me.lucko.spark.fabric;

import me.lucko.spark.common.tick.SimpleTickReporter;
import me.lucko.spark.common.tick.TickReporter;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;

public abstract class FabricTickReporter extends SimpleTickReporter implements TickReporter {

    public static final class Server extends FabricTickReporter implements ServerTickEvents.StartTick, ServerTickEvents.EndTick {
        @Override
        public void onStartTick(MinecraftServer minecraftServer) {
            onStart();
        }

        @Override
        public void onEndTick(MinecraftServer minecraftServer) {
            onEnd();
        }

        @Override
        public void start() {
            ServerTickEvents.START_SERVER_TICK.register(this);
            ServerTickEvents.END_SERVER_TICK.register(this);
        }
    }

    public static final class Client extends FabricTickReporter implements ClientTickEvents.StartTick, ClientTickEvents.EndTick {
        @Override
        public void onStartTick(MinecraftClient minecraftClient) {
            onStart();
        }

        @Override
        public void onEndTick(MinecraftClient minecraftClient) {
            onEnd();
        }

        @Override
        public void start() {
            ClientTickEvents.START_CLIENT_TICK.register(this);
            ClientTickEvents.END_CLIENT_TICK.register(this);
        }
    }
}
