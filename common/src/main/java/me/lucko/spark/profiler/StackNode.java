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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a node in the overall sampling stack.
 *
 * <p>The base implementation of this class is only used for the root of node structures. The
 * {@link StackTraceNode} class is used for representing method calls in the structure.</p>
 */
public class StackNode implements Comparable<StackNode> {

    private static final int MAX_STACK_DEPTH = 300;

    /**
     * The name of this node
     */
    private final String name;

    /**
     * A map of this nodes children
     */
    private final Map<String, StackNode> children = new HashMap<>();

    /**
     * The accumulated sample time for this node
     */
    private long totalTime = 0;

    public StackNode(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Collection<StackNode> getChildren() {
        if (this.children.isEmpty()) {
            return Collections.emptyList();
        }

        List<StackNode> list = new ArrayList<>(this.children.values());
        list.sort(null);
        return list;
    }
    
    private StackNode resolveChild(String name) {
        return this.children.computeIfAbsent(name, StackNode::new);
    }
    
    private StackNode resolveChild(String className, String methodName) {
        return this.children.computeIfAbsent(StackTraceNode.formName(className, methodName), name -> new StackTraceNode(className, methodName));
    }
    
    public long getTotalTime() {
        return this.totalTime;
    }

    public void accumulateTime(long time) {
        this.totalTime += time;
    }
    
    private void log(StackTraceElement[] elements, int skip, long time) {
        accumulateTime(time);

        if (skip >= MAX_STACK_DEPTH) {
            return;
        }
        
        if (elements.length - skip == 0) {
            return;
        }
        
        StackTraceElement bottom = elements[elements.length - (skip + 1)];
        resolveChild(bottom.getClassName(), bottom.getMethodName()).log(elements, skip + 1, time);
    }
    
    public void log(StackTraceElement[] elements, long time) {
        log(elements, 0, time);
    }

    @Override
    public int compareTo(StackNode o) {
        return getName().compareTo(o.getName());
    }

    public JsonObject serialize() {
        JsonObject ret = new JsonObject();

        // append metadata about this node
        appendMetadata(ret);

        // include the total time recorded for this node
        ret.addProperty("totalTime", getTotalTime());

        // append child nodes, if any are present
        Collection<StackNode> childNodes = getChildren();
        if (!childNodes.isEmpty()) {
            JsonArray children = new JsonArray();
            for (StackNode child : childNodes) {
                children.add(child.serialize());
            }
            ret.add("children", children);
        }

        return ret;
    }

    protected void appendMetadata(JsonObject obj) {
        obj.addProperty("name", getName());
    }

}
