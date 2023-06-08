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

package me.lucko.spark.common.util;

import net.bytebuddy.agent.ByteBuddyAgent;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses {@link Instrumentation} to find a class reference for given class names.
 *
 * <p>This is necessary as we don't always have access to the classloader for a given class.</p>
 */
public class ClassFinder {

    private final Map<String, Class<?>> classes = new HashMap<>();

    public ClassFinder() {
        Instrumentation instrumentation;
        try {
            instrumentation = ByteBuddyAgent.install();
        } catch (Exception e) {
            return;
        }
        if (instrumentation == null) {
            return;
        }

        // obtain and cache loaded classes
        for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
            this.classes.put(loadedClass.getName(), loadedClass);
        }
    }

    public @Nullable Class<?> findClass(String className) {
        // try instrumentation
        Class<?> clazz = this.classes.get(className);
        if (clazz != null) {
            return clazz;
        }

        // try Class.forName
        try {
            return Class.forName(className);
        } catch (Throwable e) {
            // ignore
        }

        return null;
    }

}
