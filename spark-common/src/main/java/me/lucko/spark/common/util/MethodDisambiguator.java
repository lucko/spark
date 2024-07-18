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

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import me.lucko.spark.common.sampler.node.StackTraceNode;
import me.lucko.spark.common.util.classfinder.ClassFinder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to disambiguate a method call (class + method name + line)
 * to a method (method name + method description).
 */
public final class MethodDisambiguator {
    private final ClassFinder classFinder;
    private final Map<String, ComputedClass> cache;

    public MethodDisambiguator(ClassFinder classFinder) {
        this.classFinder = classFinder;
        this.cache = new ConcurrentHashMap<>();
    }

    public Optional<MethodDescription> disambiguate(StackTraceNode element) {
        String desc = element.getMethodDescription();
        if (desc != null) {
            return Optional.of(new MethodDescription(element.getMethodName(), desc));
        }

        return disambiguate(element.getClassName(), element.getMethodName(), element.getLineNumber());
    }

    public Optional<MethodDescription> disambiguate(String className, String methodName, int lineNumber) {
        ComputedClass computedClass = this.cache.get(className);
        if (computedClass == null) {
            try {
                computedClass = compute(className);
            } catch (Throwable e) {
                computedClass = ComputedClass.EMPTY;
            }

            // harmless race
            this.cache.put(className, computedClass);
        }

        List<MethodDescription> descriptions = computedClass.descriptionsByName.get(methodName);
        switch (descriptions.size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(descriptions.get(0));
            default:
                return Optional.ofNullable(computedClass.descriptionsByLine.get(lineNumber));
        }
    }

    private ClassReader getClassReader(String className) throws IOException {
        String resource = className.replace('.', '/') + ".class";

        try (InputStream is = ClassLoader.getSystemResourceAsStream(resource)) {
            if (is != null) {
                return new ClassReader(is);
            }
        }

        Class<?> clazz = this.classFinder.findClass(className);
        if (clazz != null) {
            try (InputStream is = clazz.getClassLoader().getResourceAsStream(resource)) {
                if (is != null) {
                    return new ClassReader(is);
                }
            }
        }

        throw new IOException("Unable to get resource: " + className);
    }

    private ComputedClass compute(String className) throws IOException {
        ImmutableListMultimap.Builder<String, MethodDescription> descriptionsByName = ImmutableListMultimap.builder();
        Map<Integer, MethodDescription> descriptionsByLine = new HashMap<>();

        getClassReader(className).accept(new ClassVisitor(Opcodes.ASM7) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodDescription description = new MethodDescription(name, descriptor);
                descriptionsByName.put(name, description);

                return new MethodVisitor(Opcodes.ASM7) {
                    @Override
                    public void visitLineNumber(int line, Label start) {
                        descriptionsByLine.put(line, description);
                    }
                };
            }
        }, Opcodes.ASM7);

        return new ComputedClass(descriptionsByName.build(), ImmutableMap.copyOf(descriptionsByLine));
    }

    private static final class ComputedClass {
        private static final ComputedClass EMPTY = new ComputedClass(ImmutableListMultimap.of(), ImmutableMap.of());

        private final ListMultimap<String, MethodDescription> descriptionsByName;
        private final Map<Integer, MethodDescription> descriptionsByLine;

        private ComputedClass(ListMultimap<String, MethodDescription> descriptionsByName, Map<Integer, MethodDescription> descriptionsByLine) {
            this.descriptionsByName = descriptionsByName;
            this.descriptionsByLine = descriptionsByLine;
        }
    }

    public static final class MethodDescription {
        private final String name;
        private final String desc;

        private MethodDescription(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        public String getName() {
            return this.name;
        }

        public String getDesc() {
            return this.desc;
        }

        @Override
        public String toString() {
            return this.name + this.desc;
        }
    }

}
