/*
 * WarmRoast
 * Copyright (C) 2013 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package me.lucko.spark.profiler;

import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Represents a {@link StackNode node} for a method call.
 */
public class StackTraceNode extends StackNode {

    /**
     * Forms the {@link StackNode#getName()} for a {@link StackTraceNode}.
     *
     * @param className the name of the class
     * @param methodName the name of the method
     * @return the name
     */
    static String formName(String className, String methodName) {
        return className + "." + methodName + "()";
    }

    /** The name of the class */
    private final String className;
    /** The name of the method */
    private final String methodName;

    public StackTraceNode(String className, String methodName) {
        super(formName(className, methodName));
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() {
        return this.className;
    }

    public String getMethodName() {
        return this.methodName;
    }

    @Override
    protected void appendMetadata(JsonWriter writer) throws IOException {
        writer.name("className").value(this.className);
        writer.name("methodName").value(this.methodName);
    }

    @Override
    public int compareTo(StackNode that) {
        return Long.compare(that.getTotalTime(), this.getTotalTime());
    }

}
