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

import java.util.concurrent.CompletableFuture;

/**
 * Base implementation class for {@link Sampler}s.
 */
public abstract class AbstractSampler implements Sampler {

    /** The interval to wait between sampling, in microseconds */
    protected final int interval;

    /** The instance used to generate thread information for use in sampling */
    protected final ThreadDumper threadDumper;

    /** The time when sampling first began */
    protected long startTime = -1;

    /** The unix timestamp (in millis) when this sampler should automatically complete. */
    protected final long endTime; // -1 for nothing

    /** A future to encapsulate the completion of this sampler instance */
    protected final CompletableFuture<Sampler> future = new CompletableFuture<>();

    protected AbstractSampler(int interval, ThreadDumper threadDumper, long endTime) {
        this.interval = interval;
        this.threadDumper = threadDumper;
        this.endTime = endTime;
    }

    @Override
    public long getStartTime() {
        if (this.startTime == -1) {
            throw new IllegalStateException("Not yet started");
        }
        return this.startTime;
    }

    @Override
    public long getEndTime() {
        return this.endTime;
    }

    @Override
    public CompletableFuture<Sampler> getFuture() {
        return this.future;
    }
}
