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

package me.lucko.spark.sampler;

/**
 * A hook with the game's "tick loop".
 */
public interface TickCounter extends AutoCloseable {

    /**
     * Starts the counter
     */
    void start();

    /**
     * Stops the counter
     */
    @Override
    void close();

    /**
     * Gets the current tick number
     *
     * @return the current tick
     */
    long getCurrentTick();

    /**
     * Adds a task to be called each time the tick increments
     *
     * @param runnable the task
     */
    void addTickTask(Runnable runnable);

    /**
     * Removes a tick task
     *
     * @param runnable the task
     */
    void removeTickTask(Runnable runnable);

}
