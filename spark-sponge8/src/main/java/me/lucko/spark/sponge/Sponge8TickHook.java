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

import org.spongepowered.api.Game;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.plugin.PluginContainer;

public class Sponge8TickHook extends AbstractTickHook implements TickHook, Runnable {
    private final PluginContainer plugin;
    private final Game game;
    private ScheduledTask task;

    public Sponge8TickHook(PluginContainer plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    @Override
    public void run() {
        onTick();
    }

    @Override
    public void start() {
        Task task = Task.builder()
                .interval(Ticks.of(1))
                .name("spark-ticker")
                .plugin(plugin)
                .execute(this)
                .build();
        this.task = game.server().scheduler().submit(task);
    }

    @Override
    public void close() {
        this.task.cancel();
    }

}
