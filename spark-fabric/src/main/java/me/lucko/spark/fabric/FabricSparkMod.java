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

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class FabricSparkMod implements ModInitializer {

    private static FabricSparkMod instance;
    private final Set<FabricTickCounter> clientCounters = new HashSet<>();
    private final Set<FabricTickCounter> serverCounters = new HashSet<>();
    private String version;
    private Path configDir;
    // Use events from Fabric API later
    // Return true to abort sending to server
    private Predicate<String> chatSendCallback = s -> false;

    public FabricSparkMod() {
    }

    public static FabricSparkMod getInstance() {
        return instance;
    }

    @Override
    public void onInitialize() {
        FabricSparkMod.instance = this;
        FabricLoader loader = FabricLoader.getInstance();
        this.version = loader.getModContainer("spark")
                .orElseThrow(() -> new IllegalStateException("Spark loaded incorrectly!"))
                .getMetadata()
                .getVersion()
                .getFriendlyString();
        this.configDir = loader.getConfigDirectory().toPath().resolve("spark");

        // When Fabric API is available, we will register event listeners here
    }

    public String getVersion() {
        return version;
    }

    public Path getConfigDirectory() {
        return configDir;
    }

    public void setChatSendCallback(Predicate<String> callback) {
        this.chatSendCallback = callback;
    }

    public boolean tryProcessChat(String message) {
        return chatSendCallback.test(message);
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
        for (FabricTickCounter each : clientCounters) {
            each.onTick();
        }
    }

    public void tickServerCounters() {
        for (FabricTickCounter each : serverCounters) {
            each.onTick();
        }
    }
}
