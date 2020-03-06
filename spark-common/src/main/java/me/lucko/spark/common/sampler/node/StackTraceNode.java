/*
 * This file is part of spark.
 *
 *  Copyright (C) Albert Pham <http://www.sk89q.com>
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

import me.lucko.spark.common.util.MethodDisambiguator;
import me.lucko.spark.proto.SparkProtos;

import java.util.Objects;

/**
 * Represents a stack trace element within the {@link AbstractNode node} structure.
 */
public final class StackTraceNode extends AbstractNode implements Comparable<StackTraceNode> {

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

    public int getLineNumber() {
        return this.description.lineNumber;
    }

    public int getParentLineNumber() {
        return this.description.parentLineNumber;
    }

    public SparkProtos.StackTraceNode toProto(MergeMode mergeMode) {
        SparkProtos.StackTraceNode.Builder proto = SparkProtos.StackTraceNode.newBuilder()
                .setTime(getTotalTime())
                .setClassName(this.description.className)
                .setMethodName(this.description.methodName);

        if (this.description.lineNumber >= 0) {
            proto.setLineNumber(this.description.lineNumber);
        }

        if (mergeMode.separateParentCalls() && this.description.parentLineNumber >= 0) {
            proto.setParentLineNumber(this.description.parentLineNumber);
        }

        mergeMode.getMethodDisambiguator().disambiguate(this)
                .map(MethodDisambiguator.MethodDescription::getDesc)
                .ifPresent(proto::setMethodDesc);

        for (StackTraceNode child : exportChildren(mergeMode)) {
            proto.addChildren(child.toProto(mergeMode));
        }

        return proto.build();
    }

    @Override
    public int compareTo(StackTraceNode that) {
        if (this == that) {
            return 0;
        }

        int i = -Double.compare(this.getTotalTime(), that.getTotalTime());
        if (i != 0) {
            return i;
        }

        return this.description.compareTo(that.description);
    }

    /**
     * Encapsulates the attributes of a {@link StackTraceNode}.
     */
    public static final class Description implements Comparable<Description> {
        private final String className;
        private final String methodName;
        private final int lineNumber;
        private final int parentLineNumber;

        private final int hash;

        public Description(String className, String methodName, int lineNumber, int parentLineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
            this.parentLineNumber = parentLineNumber;
            this.hash = Objects.hash(this.className, this.methodName, this.lineNumber, this.parentLineNumber);
        }

        @Override
        public int compareTo(Description that) {
            if (this == that) {
                return 0;
            }

            int i = this.className.compareTo(that.className);
            if (i != 0) {
                return i;
            }

            i = this.methodName.compareTo(that.methodName);
            if (i != 0) {
                return i;
            }

            i = Integer.compare(this.lineNumber, that.lineNumber);
            if (i != 0) {
                return i;
            }

            return Integer.compare(this.parentLineNumber, that.parentLineNumber);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Description description = (Description) o;
            return this.lineNumber == description.lineNumber &&
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
