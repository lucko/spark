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

package me.lucko.spark.common.sampler.node;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * Represents a stack trace element within the {@link AbstractNode node} structure.
 */
public final class StackTraceNode extends AbstractNode {

    /**
     * Magic number to denote "no present" line number for a node.
     */
    public static final int NULL_LINE_NUMBER = -1;

    /** A description of the element */
    private final Description description;

    public StackTraceNode(Description description) {
        this.description = description;
    }

    public String getClassName() {
        return this.description.className();
    }

    public String getMethodName() {
        return this.description.methodName();
    }

    public String getMethodDescription() {
        return this.description instanceof AsyncDescription
                ? ((AsyncDescription) this.description).methodDescription()
                : null;
    }

    public int getLineNumber() {
        return this.description instanceof JavaDescription
                ? ((JavaDescription) this.description).lineNumber()
                : NULL_LINE_NUMBER;
    }

    public int getParentLineNumber() {
        return this.description instanceof JavaDescription
                ? ((JavaDescription) this.description).parentLineNumber()
                : NULL_LINE_NUMBER;
    }

    /**
     * Function to construct a {@link Description} from a stack trace element
     * of type {@code T}.
     *
     * @param <T> the stack trace element type, e.g. {@link java.lang.StackTraceElement}
     */
    @FunctionalInterface
    public interface Describer<T> {

        /**
         * Create a description for the given element.
         *
         * @param element the element
         * @param parent the parent element
         * @return the description
         */
        Description describe(T element, @Nullable T parent);
    }

    public interface Description {
        String className();

        String methodName();
    }

    public static final class AsyncDescription implements Description {
        private final String className;
        private final String methodName;
        private final String methodDescription;

        private final int hash;

        public AsyncDescription(String className, String methodName, String methodDescription) {
            this.className = className;
            this.methodName = methodName;
            this.methodDescription = methodDescription;
            this.hash = Objects.hash(this.className, this.methodName, this.methodDescription);
        }

        @Override
        public String className() {
            return this.className;
        }

        @Override
        public String methodName() {
            return this.methodName;
        }

        public String methodDescription() {
            return this.methodDescription;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AsyncDescription description = (AsyncDescription) o;
            return this.hash == description.hash &&
                    this.className.equals(description.className) &&
                    this.methodName.equals(description.methodName) &&
                    Objects.equals(this.methodDescription, description.methodDescription);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }
    }

    public static final class JavaDescription implements Description {
        private final String className;
        private final String methodName;
        private final int lineNumber;
        private final int parentLineNumber;

        private final int hash;

        public JavaDescription(String className, String methodName, int lineNumber, int parentLineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
            this.parentLineNumber = parentLineNumber;
            this.hash = Objects.hash(this.className, this.methodName, this.lineNumber, this.parentLineNumber);
        }

        @Override
        public String className() {
            return this.className;
        }

        @Override
        public String methodName() {
            return this.methodName;
        }

        public int lineNumber() {
            return this.lineNumber;
        }

        public int parentLineNumber() {
            return this.parentLineNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JavaDescription description = (JavaDescription) o;
            return this.hash == description.hash &&
                    this.lineNumber == description.lineNumber &&
                    this.parentLineNumber == description.parentLineNumber &&
                    this.className.equals(description.className) &&
                    this.methodName.equals(description.methodName);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }
    }

}
