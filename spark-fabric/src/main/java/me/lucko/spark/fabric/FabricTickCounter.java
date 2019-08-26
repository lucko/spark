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

import me.lucko.spark.common.sampler.TickCounter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class FabricTickCounter implements TickCounter {

    private final Set<TickTask> tasks = new HashSet<>();
    private final Consumer<FabricTickCounter> adder;
    private final Consumer<FabricTickCounter> remover;
    private int tick = 0;

    public FabricTickCounter(Consumer<FabricTickCounter> adder, Consumer<FabricTickCounter> remover) {
        this.adder = adder;
        this.remover = remover;
    }

    public void onTick() {
        for (TickTask r : this.tasks) {
            r.onTick(this);
        }
        this.tick++;
    }

    @Override
    public void start() {
        this.adder.accept(this);
    }

    @Override
    public void close() {
        this.remover.accept(this);
    }

    @Override
    public int getCurrentTick() {
        return this.tick;
    }

    @Override
    public void addTickTask(TickTask runnable) {
        this.tasks.add(runnable);
    }

    @Override
    public void removeTickTask(TickTask runnable) {
        this.tasks.remove(runnable);
    }
}
