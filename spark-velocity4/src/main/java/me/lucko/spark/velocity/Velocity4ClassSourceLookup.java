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

package me.lucko.spark.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;

import me.lucko.spark.common.util.ClassSourceLookup;

import org.checkerframework.checker.nullness.qual.Nullable;

public class Velocity4ClassSourceLookup extends ClassSourceLookup.ByClassLoader {
    private static final Class<?> PLUGIN_CLASS_LOADER;

    static {
        try {
            PLUGIN_CLASS_LOADER = Class.forName("com.velocitypowered.proxy.plugin.PluginClassLoader");
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final PluginManager pluginManager;

    public Velocity4ClassSourceLookup(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public @Nullable String identify(ClassLoader loader) {
        if (PLUGIN_CLASS_LOADER.isInstance(loader)) {
            for (PluginContainer plugin : this.pluginManager.plugins()) {
                Object instance = plugin.instance();
                if (instance != null && instance.getClass().getClassLoader() == loader) {
                    return plugin.description().name();
                }
            }
        }
        return null;
    }
}
