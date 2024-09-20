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

package me.lucko.spark.sponge;

import me.lucko.spark.common.tick.AbstractTickHook;
import me.lucko.spark.common.tick.TickHook;
import org.spongepowered.api.scheduler.Task;

public class Sponge7TickHook extends AbstractTickHook implements TickHook, Runnable {
    private final Sponge7SparkPlugin plugin;
    private Task task;

    public Sponge7TickHook(Sponge7SparkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        onTick();
    }

    @Override
    public void start() {
        this.task = Task.builder().intervalTicks(1).name("spark-ticker").execute(this).submit(this.plugin);
    }

    @Override
    public void close() {
        this.task.cancel();
    }

}
