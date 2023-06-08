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

package me.lucko.spark.fabric.smap;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.MixinService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches the lookup of class -> source debug info for classes loaded on the JVM.
 *
 * The {@link me.lucko.spark.fabric.plugin.FabricSparkMixinPlugin} also supplements this cache with
 * extra information as classes are exported.
 */
public enum SourceDebugCache {
    ;

    // class name -> smap
    private static final Map<String, SmapValue> CACHE = new ConcurrentHashMap<>();

    public static void put(String className, ClassNode node) {
        if (className == null || node == null) {
            return;
        }
        className = className.replace('/', '.');
        CACHE.put(className, SmapValue.of(node.sourceDebug));
    }

    public static String getSourceDebugInfo(String className) {
        SmapValue cached = CACHE.get(className);
        if (cached != null) {
            return cached.value();
        }

        try {
            IClassBytecodeProvider provider = MixinService.getService().getBytecodeProvider();
            ClassNode classNode = provider.getClassNode(className.replace('.', '/'));

            if (classNode != null) {
                put(className, classNode);
                return classNode.sourceDebug;
            }

        } catch (Exception e) {
            // ignore
        }

        CACHE.put(className, SmapValue.NULL);
        return null;
    }

    private record SmapValue(String value) {
        static final SmapValue NULL = new SmapValue(null);

        static SmapValue of(String value) {
            if (value == null) {
                return NULL;
            } else {
                return new SmapValue(value);
            }
        }

    }

}
