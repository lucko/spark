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

package me.lucko.spark.common.api;

import me.lucko.spark.api.gc.GarbageCollector;
import me.lucko.spark.common.monitor.memory.GarbageCollectorStatistics;

import org.checkerframework.checker.nullness.qual.NonNull;

public class GarbageCollectorInfo implements GarbageCollector {
    private final String name;
    private final long totalCollections;
    private final long totalTime;
    private final double averageTime;
    private final long averageFrequency;

    public GarbageCollectorInfo(String name, GarbageCollectorStatistics stats, long serverUptime) {
        this.name = name;
        this.totalCollections = stats.getCollectionCount();
        this.totalTime = stats.getCollectionTime();
        this.averageTime = stats.getAverageCollectionTime();
        this.averageFrequency = stats.getAverageCollectionFrequency(serverUptime);
    }

    @Override
    public @NonNull String name() {
        return this.name;
    }

    @Override
    public long totalCollections() {
        return this.totalCollections;
    }

    @Override
    public long totalTime() {
        return this.totalTime;
    }

    @Override
    public double avgTime() {
        return this.averageTime;
    }

    @Override
    public long avgFrequency() {
        return this.averageFrequency;
    }
}
