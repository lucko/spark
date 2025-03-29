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

import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.util.JavaVersion;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Uses {@link Instrumentation} to find a class reference for given class names.
 *
 * <p>This is necessary as we don't always have access to the classloader for a given class.</p>
 */
public class InstrumentationClassFinder implements ClassFinder {

    private static boolean warned = false;

    private static Instrumentation loadInstrumentation(SparkPlugin plugin) {
        Instrumentation instrumentation = null;
        try {
            instrumentation = ByteBuddyAgent.install();
            if (!warned && JavaVersion.getJavaVersion() >= 21) {
                warned = true;
                plugin.log(Level.INFO, "If you see a warning above that says \"WARNING: A Java agent has been loaded dynamically\", it can be safely ignored.");
                plugin.log(Level.INFO, "See here for more information: https://spark.lucko.me/docs/misc/Java-agent-warning");
            }
        } catch (Exception e) {
            // ignored
        }
        return instrumentation;
    }

    private final Map<String, Class<?>> classes = new HashMap<>();

    public InstrumentationClassFinder(SparkPlugin plugin) {
        this(loadInstrumentation(plugin));
    }

    public InstrumentationClassFinder(Instrumentation instrumentation) {
        if (instrumentation == null) {
            return;
        }

        // obtain and cache loaded classes
        for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
            this.classes.put(loadedClass.getName(), loadedClass);
        }
    }

    @Override
    public @Nullable Class<?> findClass(String className) {
        return this.classes.get(className);
    }

}
