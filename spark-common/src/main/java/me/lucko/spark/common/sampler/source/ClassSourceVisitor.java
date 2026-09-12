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

package me.lucko.spark.common.sampler.source;

import me.lucko.spark.common.sampler.node.StackTraceNode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.common.util.classfinder.ClassFinder;
import me.lucko.spark.proto.SparkSamplerProtos;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A visitor which scans the {@link StackTraceNode}s (contained in {@link ThreadNode}s)
 * and accumulates class source identifiers using a {@link ClassSourceLookup}.
 */
public interface ClassSourceVisitor {

    static ClassSourceVisitor create(ClassSourceLookup lookup, Supplier<ClassFinder> classFinderSupplier) {
        if (lookup == ClassSourceLookup.NO_OP) {
            return NoOp.INSTANCE; // don't bother!
        }
        return new Impl(lookup, classFinderSupplier.get());
    }

    void visit(ThreadNode node);

    void exportToProto(SparkSamplerProtos.SamplerData.Builder proto);

    enum NoOp implements ClassSourceVisitor {
        INSTANCE;

        @Override
        public void visit(ThreadNode node) {

        }

        @Override
        public void exportToProto(SparkSamplerProtos.SamplerData.Builder proto) {

        }
    }

    class Impl implements ClassSourceVisitor {
        private final ClassSourceLookup lookup;
        private final ClassFinder classFinder;

        private final SourcesMap<String> classSources = new SourcesMap<>(Function.identity());
        private final SourcesMap<ClassSourceLookup.MethodCall> methodSources = new SourcesMap<>(ClassSourceLookup.MethodCall::toString);
        private final SourcesMap<ClassSourceLookup.MethodCallByLine> lineSources = new SourcesMap<>(ClassSourceLookup.MethodCallByLine::toString);

        Impl(ClassSourceLookup lookup, ClassFinder classFinder) {
            this.lookup = lookup;
            this.classFinder = classFinder;
        }

        @Override
        public void visit(ThreadNode node) {
            Queue<StackTraceNode> queue = new ArrayDeque<>(node.getChildren());
            for (StackTraceNode n = queue.poll(); n != null; n = queue.poll()) {
                visitStackNode(n);
                queue.addAll(n.getChildren());
            }
        }

        private void visitStackNode(StackTraceNode node) {
            this.classSources.computeIfAbsent(
                    node.getClassName(),
                    className -> {
                        Class<?> clazz = this.classFinder.findClass(className);
                        if (clazz == null) {
                            return null;
                        }
                        return this.lookup.identify(clazz);
                    });

            if (!this.lookup.supportsIdentifyingMethodCalls()) {
                return;
            }

            if (node.getMethodDescription() != null) {
                ClassSourceLookup.MethodCall methodCall = new ClassSourceLookup.MethodCall(node.getClassName(), node.getMethodName(), node.getMethodDescription());
                this.methodSources.computeIfAbsent(methodCall, this.lookup::identify);
            } else if (node.getLineNumber() != StackTraceNode.NULL_LINE_NUMBER) {
                ClassSourceLookup.MethodCallByLine methodCall = new ClassSourceLookup.MethodCallByLine(node.getClassName(), node.getMethodName(), node.getLineNumber());
                this.lineSources.computeIfAbsent(methodCall, this.lookup::identify);
            }
        }

        @Override
        public void exportToProto(SparkSamplerProtos.SamplerData.Builder proto) {
            if (this.classSources.hasMappings()) {
                proto.putAllClassSources(this.classSources.export());
            }

            if (this.methodSources.hasMappings()) {
                proto.putAllMethodSources(this.methodSources.export());
            }

            if (this.lineSources.hasMappings()) {
                proto.putAllLineSources(this.lineSources.export());
            }
        }
    }

    final class SourcesMap<T> {
        // <key> --> identifier (plugin name)
        private final Map<T, String> map = new HashMap<>();
        private final Function<? super T, String> keyToStringFunction;

        private SourcesMap(Function<? super T, String> keyToStringFunction) {
            this.keyToStringFunction = keyToStringFunction;
        }

        public void computeIfAbsent(T key, ComputeFunction<T> function) {
            if (!this.map.containsKey(key)) {
                try {
                    this.map.put(key, function.compute(key));
                } catch (Throwable e) {
                    this.map.put(key, null);
                }
            }
        }

        public boolean hasMappings() {
            this.map.values().removeIf(Objects::isNull);
            return !this.map.isEmpty();
        }

        public Map<String, String> export() {
            this.map.values().removeIf(Objects::isNull);
            if (this.keyToStringFunction.equals(Function.identity())) {
                //noinspection unchecked
                return (Map<String, String>) this.map;
            } else {
                return this.map.entrySet().stream().collect(Collectors.toMap(
                        e -> this.keyToStringFunction.apply(e.getKey()),
                        Map.Entry::getValue
                ));
            }
        }

        @FunctionalInterface
        public interface ComputeFunction<T> {
            String compute(T key) throws Exception;
        }
    }
}
