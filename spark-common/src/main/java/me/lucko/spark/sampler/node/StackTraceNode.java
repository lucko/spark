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

package me.lucko.spark.sampler.node;

import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Represents a stack trace element within the {@link AbstractNode node} structure.
 */
public final class StackTraceNode extends AbstractNode implements Comparable<StackTraceNode> {

    /**
     * Forms a key to represent the given node.
     *
     * @param className the name of the class
     * @param methodName the name of the method
     * @return the key
     */
    static String generateKey(String className, String methodName) {
        return className + "." + methodName;
    }

    /** The name of the class */
    private final String className;
    /** The name of the method */
    private final String methodName;

    public StackTraceNode(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    protected void appendMetadata(JsonWriter writer) throws IOException {
        writer.name("cl").value(this.className);
        writer.name("m").value(this.methodName);
    }

    private String key() {
        return generateKey(this.className, this.methodName);
    }

    @Override
    public int compareTo(StackTraceNode that) {
        int i = -Long.compare(this.getTotalTime(), that.getTotalTime());
        if (i != 0) {
            return i;
        }

        return this.key().compareTo(that.key());
    }

}
