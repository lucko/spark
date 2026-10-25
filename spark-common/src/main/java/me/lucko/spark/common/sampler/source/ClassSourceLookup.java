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
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Objects;
import java.util.logging.Level;

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
     * Whether this lookup supports identifying method calls.
     *
     * @return true if method calls are supported, false otherwise
     */
    default boolean supportsIdentifyingMethodCalls() {
        return false;
    }

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
    ClassSourceLookup NO_OP = clazz -> null;

    static ClassSourceLookup create(SparkPlatform platform) {
        try {
            return platform.createClassSourceLookup();
        } catch (Exception e) {
            platform.getPlugin().log(Level.WARNING, "Failed to create ClassSourceLookup", e);
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
     * A {@link ClassSourceLookup} which identifies classes based on their {@link ProtectionDomain#getCodeSource()}.
     */
    abstract class ByCodeSource implements ClassSourceLookup {

        public abstract @Nullable String identify(Path path) throws Exception;

        @Override
        public final @Nullable String identify(Class<?> clazz) throws Exception {
            ProtectionDomain protectionDomain = clazz.getProtectionDomain();
            if (protectionDomain == null) {
                return null;
            }
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource == null) {
                return null;
            }

            URL url = codeSource.getLocation();
            if (url == null) {
                return null;
            }

            return identifyUrl(url);
        }

        private String identifyUrl(URL url) throws Exception {
            switch (url.getProtocol()) {
                case "file":
                    return identify(Paths.get(url.toURI()).toAbsolutePath().normalize());
                case "jar":
                    return identifyUrl(new URL(url.getPath().split("!")[0]));
                default:
                    return null;
            }
        }

        protected static String formatFileName(String fileName) {
            return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : null;
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
