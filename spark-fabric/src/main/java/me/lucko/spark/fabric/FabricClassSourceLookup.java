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

import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.util.ClassFinder;
import me.lucko.spark.fabric.smap.MixinUtils;
import me.lucko.spark.fabric.smap.SourceMap;
import me.lucko.spark.fabric.smap.SourceMapProvider;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public class FabricClassSourceLookup extends ClassSourceLookup.ByCodeSource {

    private final ClassFinder classFinder = new ClassFinder();
    private final SourceMapProvider smapProvider = new SourceMapProvider();

    private final Path modsDirectory;
    private final Map<String, String> pathToModMap;

    public FabricClassSourceLookup() {
        FabricLoader loader = FabricLoader.getInstance();
        this.modsDirectory = loader.getGameDir().resolve("mods").toAbsolutePath().normalize();
        this.pathToModMap = constructPathToModIdMap(loader.getAllMods());
    }

    @Override
    public String identifyFile(Path path) {
        String id = this.pathToModMap.get(path.toAbsolutePath().normalize().toString());
        if (id != null) {
            return id;
        }

        if (!path.startsWith(this.modsDirectory)) {
            return null;
        }

        return super.identifyFileName(this.modsDirectory.relativize(path).toString());
    }

    @Override
    public String identify(MethodCall methodCall) throws Exception {
        String className = methodCall.getClassName();
        String methodName = methodCall.getMethodName();
        String methodDesc = methodCall.getMethodDescriptor();

        if (className.equals("native") || methodName.equals("<init>") || methodName.equals("<clinit>")) {
            return null;
        }

        Class<?> clazz = this.classFinder.findClass(className);
        if (clazz == null) {
            return null;
        }

        Class<?>[] params = getParameterTypesForMethodDesc(methodDesc);
        Method reflectMethod = clazz.getDeclaredMethod(methodName, params);

        MixinMerged mixinMarker = reflectMethod.getDeclaredAnnotation(MixinMerged.class);
        if (mixinMarker == null) {
            return null;
        }

        return modIdFromMixinClass(mixinMarker.mixin());
    }

    @Override
    public String identify(MethodCallByLine methodCall) throws Exception {
        String className = methodCall.getClassName();
        String methodName = methodCall.getMethodName();
        int lineNumber = methodCall.getLineNumber();

        if (className.equals("native") || methodName.equals("<init>") || methodName.equals("<clinit>")) {
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

        for (int fileInfoIds : inputLineInfo) {
            SourceMap.FileInfo inputFileInfo = smap.getFileInfo().get(fileInfoIds);
            if (inputFileInfo == null) {
                continue;
            }

            String path = inputFileInfo.path();
            if (path.endsWith(".java")) {
                path = path.substring(0, path.length() - 5);
            }

            String possibleMixinClassName = path.replace('/', '.');
            if (possibleMixinClassName.equals(className)) {
                continue;
            }

            return modIdFromMixinClass(possibleMixinClassName);
        }

        return null;
    }

    private static String modIdFromMixinClass(String mixinClassName) {
        for (Config config : MixinUtils.getMixinConfigs().values()) {
            IMixinConfig mixinConfig = config.getConfig();
            String mixinPackage = mixinConfig.getMixinPackage();
            if (!mixinPackage.isEmpty() && mixinClassName.startsWith(mixinPackage)) {
                return mixinConfig.getDecoration(FabricUtil.KEY_MOD_ID);
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

    private static Map<String, String> constructPathToModIdMap(Collection<ModContainer> mods) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (ModContainer mod : mods) {
            String modId = mod.getMetadata().getId();
            if (modId.equals("java")) {
                continue;
            }

            for (Path path : mod.getRootPaths()) {
                URI uri = path.toUri();
                if (uri.getScheme().equals("jar") && path.toString().equals("/")) { // ZipFileSystem
                    String zipFilePath = path.getFileSystem().toString();
                    builder.put(zipFilePath, modId);
                } else {
                    builder.put(path.toAbsolutePath().normalize().toString(), modId);
                }

            }
        }
        return builder.build();
    }
}
