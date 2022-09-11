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

import me.lucko.spark.fabric.smap.MixinUtils;
import me.lucko.spark.fabric.smap.SourceMap;
import me.lucko.spark.fabric.smap.SourceMapProvider;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FabricClassSourceLookup extends ClassSourceLookup.ByCodeSource {

    private final ClassFinder classFinder = new ClassFinder();
    private final SourceMapProvider smapProvider = new SourceMapProvider();

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
    public @Nullable String identify(MethodCall methodCall) throws Exception {
        String className = methodCall.getClassName();
        String methodName = methodCall.getMethodName();
        String methodDesc = methodCall.getMethodDescriptor();

        if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
            return null;
        }

        Class<?> clazz = this.classFinder.findClass(className);
        if (clazz == null) {
            return null;
        }

        Class<?>[] params = getParameterTypesForMethodDesc(methodDesc);
        Method reflectMethod = clazz.getDeclaredMethod(methodName, params);

        final MixinMerged mixinMarker = reflectMethod.getDeclaredAnnotation(MixinMerged.class);
        if (mixinMarker == null) {
            return null;
        }

        String mixinClassName = mixinMarker.mixin();
        Set<IMixinInfo> mixins = MixinUtils.getMixins(ClassInfo.forName(className));

        for (IMixinInfo mixin : mixins) {
            if (mixin.getClassName().equals(mixinClassName)) {
                final String modId = mixin.getConfig().getDecoration(FabricUtil.KEY_MOD_ID);
                if (modId != null) {
                    return modId;
                }
            }
        }

        return null;
    }

    @Override
    public @Nullable String identify(MethodCallByLine methodCall) throws Exception {
        String className = methodCall.getClassName();
        String methodName = methodCall.getMethodName();
        int lineNumber = methodCall.getLineNumber();

        if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
            return null;
        }

        SourceMap smap = this.smapProvider.getSourceMap(className);
        if (smap == null) {
            return null;
        }

        int[] inputLineInfo = smap.getReverseLineMapping().get(lineNumber);
        if (inputLineInfo == null || inputLineInfo.length == 0) {
            return null;
        }

        SourceMap.FileInfo inputFileInfo = smap.getFileInfo().get(inputLineInfo[0]);
        if (inputFileInfo == null) {
            return null;
        }

        IMixinInfo config = getMixinConfigFromPath(inputFileInfo.path());
        if (config == null) {
            return null;
        }

        return config.getConfig().getDecoration(FabricUtil.KEY_MOD_ID);
    }

    private static IMixinInfo getMixinConfigFromPath(String path) {
        if (path != null && path.endsWith(".java")) {
            ClassInfo info = ClassInfo.fromCache(path.substring(0, path.length() - 5));

            if (info != null && info.isMixin()) {
                Iterator<IMixinInfo> iterator = info.getAppliedMixins().iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
            }
        }
        return null;
    }

    private Class<?>[] getParameterTypesForMethodDesc(String methodDesc) {
        Type methodType = Type.getMethodType(methodDesc);
        Class<?>[] params = new Class[methodType.getArgumentTypes().length];
        Type[] argumentTypes = methodType.getArgumentTypes();

        for (int i = 0, argumentTypesLength = argumentTypes.length; i < argumentTypesLength; i++) {
            Type argumentType = argumentTypes[i];
            params[i] = getClassFromType(argumentType);
        }

        return params;
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
