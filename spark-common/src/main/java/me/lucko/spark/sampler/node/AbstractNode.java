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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Encapsulates a timed node in the sampling stack.
 */
public abstract class AbstractNode {

    private static final int MAX_STACK_DEPTH = 300;

    /**
     * A map of this nodes children
     */
    private final Map<String, StackTraceNode> children = new ConcurrentHashMap<>();

    /**
     * The accumulated sample time for this node, measured in microseconds
     */
    private final LongAdder totalTime = new LongAdder();

    /**
     * Returns the total sample time for this node in milliseconds.
     *
     * @return the total time
     */
    public long getTotalTime() {
        long millis = TimeUnit.MICROSECONDS.toMillis(this.totalTime.longValue());
        if (millis == 0) {
            return 1;
        }
        return millis;
    }

    private AbstractNode resolveChild(String className, String methodName, int lineNumber) {
        return this.children.computeIfAbsent(
                StackTraceNode.generateKey(className, methodName, lineNumber),
                name -> new StackTraceNode(className, methodName, lineNumber)
        );
    }

    public void log(StackTraceElement[] elements, long time, boolean includeLineNumbers) {
        log(elements, 0, time, includeLineNumbers);
    }
    
    private void log(StackTraceElement[] elements, int offset, long time, boolean includeLineNumbers) {
        this.totalTime.add(time);

        if (offset >= MAX_STACK_DEPTH) {
            return;
        }
        
        if (elements.length - offset == 0) {
            return;
        }

        // the first element in the array is the top of the call stack, and the last is the root
        // offset starts at 0.

        // pointer is determined by subtracting the offset from the index of the last element
        int pointer = (elements.length - 1) - offset;
        StackTraceElement element = elements[pointer];

        // the parent stack element is located at pointer+1.
        // when the current offset is 0, we know the current pointer is at the last element in the
        // array (the root) and therefore there is no parent.
        StackTraceElement parent = offset == 0 ? null : elements[pointer + 1];

        // get the line number of the parent element - the line which called "us"
        int lineNumber = parent == null || !includeLineNumbers ? StackTraceNode.NULL_LINE_NUMBER : parent.getLineNumber();

        // resolve a child element within the structure for the element at pointer
        AbstractNode child = resolveChild(element.getClassName(), element.getMethodName(), lineNumber);
        // call the log method on the found child, with an incremented offset.
        child.log(elements, offset + 1, time, includeLineNumbers);
    }

    private Collection<? extends AbstractNode> getChildren() {
        if (this.children.isEmpty()) {
            return Collections.emptyList();
        }

        List<StackTraceNode> list = new ArrayList<>(this.children.values());
        list.sort(null);
        return list;
    }

    public void serializeTo(JsonWriter writer) throws IOException {
        writer.beginObject();

        // append metadata about this node
        appendMetadata(writer);

        // include the total time recorded for this node
        writer.name("t").value(getTotalTime());

        // append child nodes, if any are present
        Collection<? extends AbstractNode> childNodes = getChildren();
        if (!childNodes.isEmpty()) {
            writer.name("c").beginArray();
            for (AbstractNode child : childNodes) {
                child.serializeTo(writer);
            }
            writer.endArray();
        }

        writer.endObject();
    }

    protected abstract void appendMetadata(JsonWriter writer) throws IOException;

}
