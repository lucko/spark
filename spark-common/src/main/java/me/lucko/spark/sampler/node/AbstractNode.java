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
     * The accumulated sample time for this node
     */
    private final LongAdder totalTime = new LongAdder();
    
    public long getTotalTime() {
        return this.totalTime.longValue();
    }

    private AbstractNode resolveChild(String className, String methodName) {
        return this.children.computeIfAbsent(
                StackTraceNode.generateKey(className, methodName),
                name -> new StackTraceNode(className, methodName)
        );
    }

    public void log(StackTraceElement[] elements, long time) {
        log(elements, 0, time);
    }
    
    private void log(StackTraceElement[] elements, int skip, long time) {
        this.totalTime.add(time);

        if (skip >= MAX_STACK_DEPTH) {
            return;
        }
        
        if (elements.length - skip == 0) {
            return;
        }
        
        StackTraceElement bottom = elements[elements.length - (skip + 1)];
        resolveChild(bottom.getClassName(), bottom.getMethodName()).log(elements, skip + 1, time);
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
