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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.node.StackTraceNode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.common.util.ClassFinder;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A function which defines the source of given {@link Class}es or (Mixin) method calls.
 */
public interface ClassSourceLookup {

    /**
     * Identify the given class.
     *
     * @param clazz the class
     * @return the source of the class
     */
    @Nullable String identify(Class<?> clazz) throws Exception;

    /**
     * Identify the given method call.
     *
     * @param methodCall the method call info
     * @return the source of the method call
     */
    default @Nullable String identify(MethodCall methodCall) throws Exception {
        return null;
    }

    /**
     * Identify the given method call.
     *
     * @param methodCall the method call info
     * @return the source of the method call
     */
    default @Nullable String identify(MethodCallByLine methodCall) throws Exception {
        return null;
    }

    /**
     * A no-operation {@link ClassSourceLookup}.
     */
    ClassSourceLookup NO_OP = new ClassSourceLookup() {
        @Override
        public @Nullable String identify(Class<?> clazz) {
            return null;
        }
    };

    static ClassSourceLookup create(SparkPlatform platform) {
        try {
            return platform.createClassSourceLookup();
        } catch (Exception e) {
            e.printStackTrace();
            return NO_OP;
        }
    }

    /**
     * A {@link ClassSourceLookup} which identifies classes based on their {@link ClassLoader}.
     */
    abstract class ByClassLoader implements ClassSourceLookup {

        public abstract @Nullable String identify(ClassLoader loader) throws Exception;

        @Override
        public final @Nullable String identify(Class<?> clazz) throws Exception {
            ClassLoader loader = clazz.getClassLoader();
            while (loader != null) {
                String source = identify(loader);
                if (source != null) {
                    return source;
                }
                loader = loader.getParent();
            }
            return null;
        }
    }

    /**
     * A {@link ClassSourceLookup} which identifies classes based on URL.
     */
    interface ByUrl extends ClassSourceLookup {

        default String identifyUrl(URL url) throws URISyntaxException, MalformedURLException {
            Path path = null;

            String protocol = url.getProtocol();
            if (protocol.equals("file")) {
                path = Paths.get(url.toURI());
            } else if (protocol.equals("jar")) {
                URL innerUrl = new URL(url.getPath());
                path = Paths.get(innerUrl.getPath().split("!")[0]);
            }

            if (path != null) {
                return identifyFile(path.toAbsolutePath().normalize());
            }

            return null;
        }

        default String identifyFile(Path path) {
            return identifyFileName(path.getFileName().toString());
        }

        default String identifyFileName(String fileName) {
            return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : null;
        }
    }

    /**
     * A {@link ClassSourceLookup} which identifies classes based on the first URL in a {@link URLClassLoader}.
     */
    class ByFirstUrlSource extends ClassSourceLookup.ByClassLoader implements ClassSourceLookup.ByUrl {
        @Override
        public @Nullable String identify(ClassLoader loader) throws IOException, URISyntaxException {
            if (loader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) loader;
                URL[] urls = urlClassLoader.getURLs();
                if (urls.length == 0) {
                    return null;
                }
                return identifyUrl(urls[0]);
            }
            return null;
        }
    }

    /**
     * A {@link ClassSourceLookup} which identifies classes based on their {@link ProtectionDomain#getCodeSource()}.
     */
    class ByCodeSource implements ClassSourceLookup, ClassSourceLookup.ByUrl {
        @Override
        public @Nullable String identify(Class<?> clazz) throws URISyntaxException, MalformedURLException {
            ProtectionDomain protectionDomain = clazz.getProtectionDomain();
            if (protectionDomain == null) {
                return null;
            }
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource == null) {
                return null;
            }

            URL url = codeSource.getLocation();
            return url == null ? null : identifyUrl(url);
        }
    }

    interface Visitor {
        void visit(ThreadNode node);

        boolean hasClassSourceMappings();

        Map<String, String> getClassSourceMapping();

        boolean hasMethodSourceMappings();

        Map<String, String> getMethodSourceMapping();

        boolean hasLineSourceMappings();

        Map<String, String> getLineSourceMapping();
    }

    static Visitor createVisitor(ClassSourceLookup lookup) {
        if (lookup == ClassSourceLookup.NO_OP) {
            return NoOpVisitor.INSTANCE; // don't bother!
        }
        return new VisitorImpl(lookup);
    }

    enum NoOpVisitor implements Visitor {
        INSTANCE;

        @Override
        public void visit(ThreadNode node) {

        }

        @Override
        public boolean hasClassSourceMappings() {
            return false;
        }

        @Override
        public Map<String, String> getClassSourceMapping() {
            return Collections.emptyMap();
        }

        @Override
        public boolean hasMethodSourceMappings() {
            return false;
        }

        @Override
        public Map<String, String> getMethodSourceMapping() {
            return Collections.emptyMap();
        }

        @Override
        public boolean hasLineSourceMappings() {
            return false;
        }

        @Override
        public Map<String, String> getLineSourceMapping() {
            return Collections.emptyMap();
        }
    }

    /**
     * Visitor which scans {@link StackTraceNode}s and accumulates class/method call identities.
     */
    class VisitorImpl implements Visitor {
        private final ClassSourceLookup lookup;
        private final ClassFinder classFinder = new ClassFinder();

        private final SourcesMap<String> classSources = new SourcesMap<>(Function.identity());
        private final SourcesMap<MethodCall> methodSources = new SourcesMap<>(MethodCall::toString);
        private final SourcesMap<MethodCallByLine> lineSources = new SourcesMap<>(MethodCallByLine::toString);

        VisitorImpl(ClassSourceLookup lookup) {
            this.lookup = lookup;
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

            if (node.getMethodDescription() != null) {
                MethodCall methodCall = new MethodCall(node.getClassName(), node.getMethodName(), node.getMethodDescription());
                this.methodSources.computeIfAbsent(methodCall, this.lookup::identify);
            } else {
                MethodCallByLine methodCall = new MethodCallByLine(node.getClassName(), node.getMethodName(), node.getLineNumber());
                this.lineSources.computeIfAbsent(methodCall, this.lookup::identify);
            }
        }

        @Override
        public boolean hasClassSourceMappings() {
            return this.classSources.hasMappings();
        }

        @Override
        public Map<String, String> getClassSourceMapping() {
            return this.classSources.export();
        }

        @Override
        public boolean hasMethodSourceMappings() {
            return this.methodSources.hasMappings();
        }

        @Override
        public Map<String, String> getMethodSourceMapping() {
            return this.methodSources.export();
        }

        @Override
        public boolean hasLineSourceMappings() {
            return this.lineSources.hasMappings();
        }

        @Override
        public Map<String, String> getLineSourceMapping() {
            return this.lineSources.export();
        }
    }

    final class SourcesMap<T> {
        // <key> --> identifier (plugin name)
        private final Map<T, String> map = new HashMap<>();
        private final Function<? super T, String> keyToStringFunction;

        private SourcesMap(Function<? super T, String> keyToStringFunction) {
            this.keyToStringFunction = keyToStringFunction;
        }

        public void computeIfAbsent(T key, ComputeSourceFunction<T> function) {
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

        private interface ComputeSourceFunction<T> {
            String compute(T key) throws Exception;
        }
    }

    /**
     * Encapsulates information about a given method call using the name + method description.
     */
    final class MethodCall {
        private final String className;
        private final String methodName;
        private final String methodDescriptor;

        public MethodCall(String className, String methodName, String methodDescriptor) {
            this.className = className;
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
        }

        public String getClassName() {
            return this.className;
        }

        public String getMethodName() {
            return this.methodName;
        }

        public String getMethodDescriptor() {
            return this.methodDescriptor;
        }

        @Override
        public String toString() {
            return this.className + ";" + this.methodName + ";" + this.methodDescriptor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodCall)) return false;
            MethodCall that = (MethodCall) o;
            return this.className.equals(that.className) &&
                    this.methodName.equals(that.methodName) &&
                    this.methodDescriptor.equals(that.methodDescriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.className, this.methodName, this.methodDescriptor);
        }
    }

    /**
     * Encapsulates information about a given method call using the name + line number.
     */
    final class MethodCallByLine {
        private final String className;
        private final String methodName;
        private final int lineNumber;

        public MethodCallByLine(String className, String methodName, int lineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }

        public String getClassName() {
            return this.className;
        }

        public String getMethodName() {
            return this.methodName;
        }

        public int getLineNumber() {
            return this.lineNumber;
        }

        @Override
        public String toString() {
            return this.className + ";" + this.lineNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodCallByLine)) return false;
            MethodCallByLine that = (MethodCallByLine) o;
            return this.lineNumber == that.lineNumber && this.className.equals(that.className);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.className, this.lineNumber);
        }
    }

}
