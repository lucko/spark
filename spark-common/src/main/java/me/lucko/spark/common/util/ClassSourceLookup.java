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

        boolean hasMappings();

        Map<String, String> getMapping();
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
        public boolean hasMappings() {
            return false;
        }

        @Override
        public Map<String, String> getMapping() {
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
        private final Map<String, String> map = new HashMap<>();

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
        public boolean hasMappings() {
            return !this.map.isEmpty();
        }

        @Override
        public Map<String, String> getMapping() {
            this.map.values().removeIf(Objects::isNull);
            return this.map;
        }

        private void visitStackNode(StackTraceNode node) {
            String className = node.getClassName();
            if (!this.map.containsKey(className)) {
                try {
                    Class<?> clazz = this.classFinder.findClass(className);
                    Objects.requireNonNull(clazz);
                    this.map.put(className, this.lookup.identify(clazz));
                } catch (Throwable e) {
                    this.map.put(className, null);
                }
            }

            // recursively
            for (StackTraceNode child : node.getChildren()) {
                visitStackNode(child);
            }
        }
    }

}
