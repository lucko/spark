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

package me.lucko.spark.common.sampler.async;

/**
 * Version of {@link StackTraceElement} for async-profiler output.
 */
public class AsyncStackTraceElement {

    /** The name of the class */
    private final String className;
    /** The name of the method */
    private final String methodName;
    /** The method description */
    private final String methodDescription;

    public AsyncStackTraceElement(String className, String methodName, String methodDescription) {
        this.className = className;
        this.methodName = methodName;
        this.methodDescription = methodDescription;
    }

    public String getClassName() {
        return this.className;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getMethodDescription() {
        return this.methodDescription;
    }
}
