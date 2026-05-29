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

package me.lucko.spark.paper;

import me.lucko.spark.common.tick.AbstractTickReporter;
import me.lucko.spark.common.tick.TickReporter;
import org.bukkit.event.Listener;

public class PaperTickReporter extends AbstractTickReporter implements TickReporter, Listener {
    private boolean open = false;

    @Override
    public void start() {
        this.open = true;
    }

    @Override
    public void close() {
        this.open = false;
    }

    @Override
    public void onTick(double duration) {
        if (this.open) {
            super.onTick(duration);
        }
    }
}
