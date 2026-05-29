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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.function.IntPredicate;

/**
 * The root of a sampling stack for a given thread / thread group.
 */
public final class ThreadNode extends AbstractNode {

    /**
     * The name of this thread / thread group
     */
    private final String name;

    /**
     * The label used to describe this thread in the viewer
     */
    public String label;

    public ThreadNode(String name) {
        this.name = name;
    }

    public String getThreadLabel() {
        return this.label != null ? this.label : this.name;
    }

    public String getThreadGroup() {
        return this.name;
    }

    public void setThreadLabel(String label) {
        this.label = label;
    }

    /**
     * Logs the given stack trace against this node and its children.
     *
     * @param describer the function that describes the elements of the stack
     * @param stack the stack
     * @param time the total time to log
     * @param window the window
     * @param <T> the stack trace element type
     */
    public <T> void log(StackTraceNode.Describer<T> describer, T[] stack, long time, int window) {
        if (stack.length == 0) {
            return;
        }

        getTimeAccumulator(window).add(time);

        AbstractNode node = this;
        T previousElement = null;

        for (int offset = 0; offset < Math.min(MAX_STACK_DEPTH, stack.length); offset++) {
            T element = stack[(stack.length - 1) - offset];

            node = node.resolveChild(describer.describe(element, previousElement));
            node.getTimeAccumulator(window).add(time);

            previousElement = element;
        }
    }

    /**
     * Removes time windows that match the given {@code predicate}.
     *
     * @param predicate the predicate to use to test the time windows
     * @return true if this node is now empty
     */
    public boolean removeTimeWindowsRecursively(IntPredicate predicate) {
        Queue<AbstractNode> queue = new ArrayDeque<>();
        queue.add(this);

        while (!queue.isEmpty()) {
            AbstractNode node = queue.remove();
            Collection<StackTraceNode> children = node.getChildren();

            boolean needToProcessChildren = false;

            for (Iterator<StackTraceNode> it = children.iterator(); it.hasNext(); ) {
                StackTraceNode child = it.next();

                boolean windowsWereRemoved = child.removeTimeWindows(predicate);
                boolean childIsNowEmpty = child.getTimeWindows().isEmpty();

                if (childIsNowEmpty) {
                    it.remove();
                    continue;
                }

                if (windowsWereRemoved) {
                    needToProcessChildren = true;
                }
            }

            if (needToProcessChildren) {
                queue.addAll(children);
            }
        }

        removeTimeWindows(predicate);
        return getTimeWindows().isEmpty();
    }

}
