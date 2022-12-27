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

package me.lucko.spark.common.sampler;

/**
 * Base settings for all samplers
 */
public class SamplerSettings {

    private final int interval;
    private final ThreadDumper threadDumper;
    private final ThreadGrouper threadGrouper;
    private final long autoEndTime;
    private final boolean runningInBackground;

    public SamplerSettings(int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper, long autoEndTime, boolean runningInBackground) {
        this.interval = interval;
        this.threadDumper = threadDumper;
        this.threadGrouper = threadGrouper;
        this.autoEndTime = autoEndTime;
        this.runningInBackground = runningInBackground;
    }

    public int interval() {
        return this.interval;
    }

    public ThreadDumper threadDumper() {
        return this.threadDumper;
    }

    public ThreadGrouper threadGrouper() {
        return this.threadGrouper;
    }

    public long autoEndTime() {
        return this.autoEndTime;
    }

    public boolean runningInBackground() {
        return this.runningInBackground;
    }
}
