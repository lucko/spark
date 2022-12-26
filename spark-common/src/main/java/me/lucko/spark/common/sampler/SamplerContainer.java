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

import java.util.concurrent.atomic.AtomicReference;

/**
 * Container for the active sampler.
 */
public class SamplerContainer implements AutoCloseable {

    private final AtomicReference<Sampler> activeSampler = new AtomicReference<>();

    /**
     * Gets the active sampler, or null if a sampler is not active.
     *
     * @return the active sampler
     */
    public Sampler getActiveSampler() {
        return this.activeSampler.get();
    }

    /**
     * Sets the active sampler, throwing an exception if another sampler is already active.
     *
     * @param sampler the sampler
     */
    public void setActiveSampler(Sampler sampler) {
        if (!this.activeSampler.compareAndSet(null, sampler)) {
            throw new IllegalStateException("Attempted to set active sampler when another was already active!");
        }
    }

    /**
     * Unsets the active sampler, if the provided sampler is active.
     *
     * @param sampler the sampler
     */
    public void unsetActiveSampler(Sampler sampler) {
        this.activeSampler.compareAndSet(sampler, null);
    }

    /**
     * Stops the active sampler, if there is one.
     */
    public void stopActiveSampler(boolean cancelled) {
        Sampler sampler = this.activeSampler.getAndSet(null);
        if (sampler != null) {
            sampler.stop(cancelled);
        }
    }

    @Override
    public void close() {
        stopActiveSampler(true);
    }

}
