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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public enum FabricSparkGameHooks {
    INSTANCE;

    private final Set<FabricTickCounter> clientCounters = new HashSet<>();
    private final Set<FabricTickCounter> serverCounters = new HashSet<>();

    // Use events from Fabric API later
    // Return true to abort sending to server
    private Predicate<String> chatSendCallback = s -> false;

    public void setChatSendCallback(Predicate<String> callback) {
        this.chatSendCallback = callback;
    }

    public boolean tryProcessChat(String message) {
        return this.chatSendCallback.test(message);
    }

    public void addClientCounter(FabricTickCounter counter) {
        this.clientCounters.add(counter);
    }

    public void removeClientCounter(FabricTickCounter counter) {
        this.clientCounters.remove(counter);
    }

    public void addServerCounter(FabricTickCounter counter) {
        this.serverCounters.add(counter);
    }

    public void removeServerCounter(FabricTickCounter counter) {
        this.serverCounters.remove(counter);
    }

    public void tickClientCounters() {
        for (FabricTickCounter counter : this.clientCounters) {
            counter.onTick();
        }
    }

    public void tickServerCounters() {
        for (FabricTickCounter counter : this.serverCounters) {
            counter.onTick();
        }
    }

}
