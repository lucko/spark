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

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

public enum MixinUtils {
    ;

    private static final Function<ClassInfo, Set<IMixinInfo>> GET_MIXINS_FUNCTION;

    static {
        Function<ClassInfo, Set<IMixinInfo>> getMixinsFunction = null;

        try {
            Method getMixins = ClassInfo.class.getDeclaredMethod("getMixins");
            getMixins.setAccessible(true);

            getMixinsFunction = classInfo -> {
                try {
                    //noinspection unchecked
                    return (Set<IMixinInfo>) getMixins.invoke(classInfo);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (Exception e) {
            // ignore
        }

        if (getMixinsFunction == null) {
            try {
                // Fabric loader >=0.12.0 proguards out the method; use the field instead
                Field mixinsField = ClassInfo.class.getDeclaredField("mixins");
                mixinsField.setAccessible(true);

                getMixinsFunction = classInfo -> {
                    try {
                        //noinspection unchecked
                        return (Set<IMixinInfo>) mixinsField.get(classInfo);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                };
            } catch (Exception e) {
                // ignore
            }
        }

        if (getMixinsFunction == null) {
            getMixinsFunction = classInfo -> Collections.emptySet();
        }

        GET_MIXINS_FUNCTION = getMixinsFunction;
    }

    public static Set<IMixinInfo> getMixins(ClassInfo classInfo) {
        return GET_MIXINS_FUNCTION.apply(classInfo);
    }
}
