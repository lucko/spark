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

package me.lucko.spark.common.tick;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class AbstractTickHook implements TickHook {

    private final Set<Callback> tasks = new CopyOnWriteArraySet<>();
    private int tick = 0;

    protected void onTick() {
        for (Callback r : this.tasks) {
            r.onTick(this.tick);
        }
        this.tick++;
    }

    @Override
    public int getCurrentTick() {
        return this.tick;
    }

    @Override
    public void addCallback(Callback runnable) {
        this.tasks.add(runnable);
    }

    @Override
    public void removeCallback(Callback runnable) {
        this.tasks.remove(runnable);
    }

}
