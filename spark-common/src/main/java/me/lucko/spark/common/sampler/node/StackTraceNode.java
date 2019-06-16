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

import me.lucko.spark.proto.SparkProtos;

/**
 * Represents a stack trace element within the {@link AbstractNode node} structure.
 */
public final class StackTraceNode extends AbstractNode implements Comparable<StackTraceNode> {

    /**
     * Magic number to denote "no present" line number for a node.
     */
    public static final int NULL_LINE_NUMBER = -1;

    /**
     * Forms a key to represent the given node.
     *
     * @param className the name of the class
     * @param methodName the name of the method
     * @param lineNumber the line number of the parent method call
     * @return the key
     */
    static String generateKey(String className, String methodName, int lineNumber) {
        return className + "." + methodName + "." + lineNumber;
    }

    /** The name of the class */
    private final String className;
    /** The name of the method */
    private final String methodName;
    /** The line number of the invocation which created this node */
    private final int lineNumber;

    public StackTraceNode(String className, String methodName, int lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
    }

    public SparkProtos.StackTraceNode toProto() {
        SparkProtos.StackTraceNode.Builder proto = SparkProtos.StackTraceNode.newBuilder()
                .setTime(getTotalTime())
                .setClassName(this.className)
                .setMethodName(this.methodName);

        if (this.lineNumber >= 0) {
            proto.setLineNumber(this.lineNumber);
        }

        for (StackTraceNode child : getChildren()) {
            proto.addChildren(child.toProto());
        }

        return proto.build();
    }

    private String key() {
        return generateKey(this.className, this.methodName, this.lineNumber);
    }

    @Override
    public int compareTo(StackTraceNode that) {
        int i = -Double.compare(this.getTotalTime(), that.getTotalTime());
        if (i != 0) {
            return i;
        }

        return this.key().compareTo(that.key());
    }

}
