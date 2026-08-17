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

package me.lucko.spark.common.command.sender;

public abstract class AbstractCommandSender<S> implements CommandSender {
    protected final S delegate;

    public AbstractCommandSender(S delegate) {
        this.delegate = delegate;
    }

    protected Object getObjectForComparison() {
        return this.delegate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractCommandSender)) return false;
        AbstractCommandSender<?> that = (AbstractCommandSender<?>) o;
        return this.getObjectForComparison().equals(that.getObjectForComparison());
    }

    @Override
    public int hashCode() {
        return this.getObjectForComparison().hashCode();
    }
}
