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

package me.lucko.spark.fabric;

import com.google.common.collect.ImmutableMap;

import me.lucko.spark.common.util.ClassFinder;
import me.lucko.spark.common.util.ClassSourceLookup;

import me.lucko.spark.fabric.smap.SMAPSourceDebugExtension;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class FabricClassSourceLookup extends ClassSourceLookup.ByCodeSource {

    private final ClassFinder classFinder = new ClassFinder();
    private final Map<String, SMAPSourceDebugExtension> smapCache = new HashMap<>();

    private final Path modsDirectory;
    private final Map<Path, String> pathToModMap;

    public FabricClassSourceLookup() {
        FabricLoader loader = FabricLoader.getInstance();
        this.modsDirectory = loader.getGameDir().resolve("mods").toAbsolutePath().normalize();
        this.pathToModMap = constructPathToModIdMap(loader.getAllMods());
    }

    @Override
    public String identifyFile(Path path) {
        String id = this.pathToModMap.get(path);
        if (id != null) {
            return id;
        }

        if (!path.startsWith(this.modsDirectory)) {
            return null;
        }

        return super.identifyFileName(this.modsDirectory.relativize(path).toString());
    }

    @Override
    public @Nullable String identify(String className, String methodName, String desc, int lineNumber) throws Exception {
        if (methodName.equals("<init>") || methodName.equals("<clinit>")) return null;

        Set<IMixinInfo> mixinInfoSet = null;
        String mixinName = null;

        if (desc != null) { // identify by descriptor
            Class<?> clazz = this.classFinder.findClass(className);
            if (clazz == null) return null;
            final Type methodType = Type.getMethodType(desc);
            Class<?>[] params = new Class[methodType.getArgumentTypes().length];
            Type[] argumentTypes = methodType.getArgumentTypes();
            for (int i = 0, argumentTypesLength = argumentTypes.length; i < argumentTypesLength; i++) {
                Type argumentType = argumentTypes[i];
                params[i] = getClassFromType(argumentType);
            }
            Method reflectMethod = clazz.getDeclaredMethod(methodName, params);

            if (reflectMethod == null) return null;

            final MixinMerged annotation = reflectMethod.getDeclaredAnnotation(MixinMerged.class);
            if (annotation == null) return null;
//        System.out.println("Found annotation " + annotation);
            mixinName = annotation.mixin();

            final ClassInfo classInfo = ClassInfo.forName(className);
//        final ClassInfo.Method method = classInfo.findMethod(methodName, desc, ClassInfo.INCLUDE_ALL);

            try {
                Method getMixins = ClassInfo.class.getDeclaredMethod("getMixins");
                getMixins.setAccessible(true);
                mixinInfoSet = (Set<IMixinInfo>) getMixins.invoke(classInfo);
            } catch (Exception e) {
                // Fabric loader >=0.12.0 proguards out this method; use the field instead
                var mixinsField = ClassInfo.class.getDeclaredField("mixins");
                mixinsField.setAccessible(true);
                mixinInfoSet = (Set<IMixinInfo>) mixinsField.get(classInfo);
            }
        } else { // identify by debug information
            final IMixinInfo config = SMAPSourceDebugExtension.getMixinConfigFor(className, lineNumber, smapCache);
            if (config == null) return null;
            mixinName = config.getClassName();
            mixinInfoSet = Set.of(config);
        }

        if (mixinInfoSet == null || mixinName == null) return null;

        for (IMixinInfo mixin : mixinInfoSet) {
            if (mixin.getClassName().equals(mixinName)) {
                final String modId = mixin.getConfig().getDecoration(FabricUtil.KEY_MOD_ID);
                if (modId != null) {
                    return modId;
                }
            }
        }

        return null;
    }

    private Class<?> getClassFromType(Type type) {
        return switch (type.getSort()) {
            case Type.VOID -> void.class;
            case Type.BOOLEAN -> boolean.class;
            case Type.CHAR -> char.class;
            case Type.BYTE -> byte.class;
            case Type.SHORT -> short.class;
            case Type.INT -> int.class;
            case Type.FLOAT -> float.class;
            case Type.LONG -> long.class;
            case Type.DOUBLE -> double.class;
            case Type.ARRAY -> {
                final Class<?> classFromType = getClassFromType(type.getElementType());
                Class<?> result = classFromType;
                if (classFromType != null) {
                    for (int i = 0; i < type.getDimensions(); i++) {
                        result = result.arrayType();
                    }
                }
                yield result;
            }
            case Type.OBJECT -> this.classFinder.findClass(type.getClassName());
            default -> null;
        };
    }

    private static Map<Path, String> constructPathToModIdMap(Collection<ModContainer> mods) {
        ImmutableMap.Builder<Path, String> builder = ImmutableMap.builder();
        for (ModContainer mod : mods) {
            for (Path path : mod.getRootPaths()) {
                builder.put(path.toAbsolutePath().normalize(), mod.getMetadata().getId());
            }
        }
        return builder.build();
    }
}
