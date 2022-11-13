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

import me.lucko.spark.common.sampler.window.ProtoTimeEncoder;
import me.lucko.spark.common.util.MethodDisambiguator;
import me.lucko.spark.proto.SparkSamplerProtos;

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
        return this.description.className;
    }

    public String getMethodName() {
        return this.description.methodName;
    }

    public String getMethodDescription() {
        return this.description.methodDescription;
    }

    public int getLineNumber() {
        return this.description.lineNumber;
    }

    public int getParentLineNumber() {
        return this.description.parentLineNumber;
    }

    public SparkSamplerProtos.StackTraceNode toProto(MergeMode mergeMode, ProtoTimeEncoder timeEncoder, Iterable<Integer> childrenRefs) {
        SparkSamplerProtos.StackTraceNode.Builder proto = SparkSamplerProtos.StackTraceNode.newBuilder()
                .setClassName(this.description.className)
                .setMethodName(this.description.methodName);

        double[] times = encodeTimesForProto(timeEncoder);
        for (double time : times) {
            proto.addTimes(time);
        }

        if (this.description.lineNumber >= 0) {
            proto.setLineNumber(this.description.lineNumber);
        }

        if (mergeMode.separateParentCalls() && this.description.parentLineNumber >= 0) {
            proto.setParentLineNumber(this.description.parentLineNumber);
        }

        if (this.description.methodDescription != null) {
            proto.setMethodDesc(this.description.methodDescription);
        } else {
            mergeMode.getMethodDisambiguator().disambiguate(this)
                    .map(MethodDisambiguator.MethodDescription::getDesc)
                    .ifPresent(proto::setMethodDesc);
        }

        proto.addAllChildrenRefs(childrenRefs);

        return proto.build();
    }

    /**
     * Function to construct a {@link StackTraceNode.Description} from a stack trace element
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

    /**
     * Encapsulates the attributes of a {@link StackTraceNode}.
     */
    public static final class Description {
        private final String className;
        private final String methodName;

        // async-profiler
        private final String methodDescription;

        // Java
        private final int lineNumber;
        private final int parentLineNumber;

        private final int hash;

        // Constructor used by the Java sampler
        public Description(String className, String methodName, int lineNumber, int parentLineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.methodDescription = null;
            this.lineNumber = lineNumber;
            this.parentLineNumber = parentLineNumber;
            this.hash = Objects.hash(this.className, this.methodName, this.lineNumber, this.parentLineNumber);
        }

        // Constructor used by the async-profiler sampler
        public Description(String className, String methodName, String methodDescription) {
            this.className = className;
            this.methodName = methodName;
            this.methodDescription = methodDescription;
            this.lineNumber = StackTraceNode.NULL_LINE_NUMBER;
            this.parentLineNumber = StackTraceNode.NULL_LINE_NUMBER;
            this.hash = Objects.hash(this.className, this.methodName, this.methodDescription);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Description description = (Description) o;
            return this.hash == description.hash &&
                    this.lineNumber == description.lineNumber &&
                    this.parentLineNumber == description.parentLineNumber &&
                    this.className.equals(description.className) &&
                    this.methodName.equals(description.methodName) &&
                    Objects.equals(this.methodDescription, description.methodDescription);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }
    }

}
