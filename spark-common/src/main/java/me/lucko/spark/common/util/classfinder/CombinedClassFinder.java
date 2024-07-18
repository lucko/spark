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

package me.lucko.spark.common.util.classfinder;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

class CombinedClassFinder implements ClassFinder {
    private final List<ClassFinder> finders;

    CombinedClassFinder(List<ClassFinder> finders) {
        this.finders = finders;
    }

    @Override
    public @Nullable Class<?> findClass(String className) {
        for (ClassFinder finder : this.finders) {
            Class<?> clazz = finder.findClass(className);
            if (clazz != null) {
                return clazz;
            }
        }
        return null;
    }
}
