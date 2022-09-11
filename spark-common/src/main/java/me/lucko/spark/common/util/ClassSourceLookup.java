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

import me.lucko.spark.common.sampler.node.StackTraceNode;
import me.lucko.spark.common.sampler.node.ThreadNode;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A function which defines the source of given {@link Class}es.
 */
public interface ClassSourceLookup {

    /**
     * Identify the given class.
     *
     * @param clazz the class
     * @return the source of the class
     */
    @Nullable String identify(Class<?> clazz) throws Exception;

    @Nullable
    default String identify(String className, String methodName, String desc, int lineNumber) throws Exception {
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
    class ByFirstUrlSource extends ByClassLoader implements ByUrl {
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
    class ByCodeSource implements ClassSourceLookup, ByUrl {
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
     * Visitor which scans {@link StackTraceNode}s and accumulates class identities.
     */
    class VisitorImpl implements Visitor {
        private final ClassSourceLookup lookup;
        private final ClassFinder classFinder = new ClassFinder();

        // class name --> identifier (plugin name)
        private final Map<String, String> classSources = new HashMap<>();

        // full method descriptor --> identifier
        private final Map<String, String> methodSources = new HashMap<>();

        // class name + line number --> identifier
        private final Map<String, String> lineSources = new HashMap<>();

        VisitorImpl(ClassSourceLookup lookup) {
            this.lookup = lookup;
        }

        @Override
        public void visit(ThreadNode node) {
            for (StackTraceNode child : node.getChildren()) {
                visitStackNode(child);
            }
        }

        @Override
        public boolean hasClassSourceMappings() {
            return !this.classSources.isEmpty();
        }

        @Override
        public Map<String, String> getClassSourceMapping() {
            this.classSources.values().removeIf(Objects::isNull);
            return this.classSources;
        }

        @Override
        public boolean hasMethodSourceMappings() {
            return !this.methodSources.isEmpty();
        }

        @Override
        public Map<String, String> getMethodSourceMapping() {
            this.methodSources.values().removeIf(Objects::isNull);
            return this.methodSources;
        }

        @Override
        public boolean hasLineSourceMappings() {
            return !this.lineSources.isEmpty();
        }

        @Override
        public Map<String, String> getLineSourceMapping() {
            this.lineSources.values().removeIf(Objects::isNull);
            return this.lineSources;
        }

        private void visitStackNode(StackTraceNode node) {
            String className = node.getClassName();
            if (!this.classSources.containsKey(className)) {
                try {
                    Class<?> clazz = this.classFinder.findClass(className);
                    Objects.requireNonNull(clazz);
                    this.classSources.put(className, this.lookup.identify(clazz));
                } catch (Throwable e) {
                    this.classSources.put(className, null);
                }
            }

            final String key = node.getMethodDescription() != null
                    ? node.getClassName() + ";" + node.getMethodName() + ";" + node.getMethodDescription()
                    : node.getClassName() + ":" + node.getLineNumber();

            final Map<String, String> sources = node.getMethodDescription() != null ? this.methodSources : this.lineSources;
            if (!sources.containsKey(key)) {
                try {
                    sources.put(key, this.lookup.identify(node.getClassName(), node.getMethodName(), node.getMethodDescription(), node.getLineNumber()));
                } catch (Throwable e) {
                    e.printStackTrace();
                    sources.put(key, null);
                }
            }

            // recursively
            for (StackTraceNode child : node.getChildren()) {
                visitStackNode(child);
            }
        }
    }

}
