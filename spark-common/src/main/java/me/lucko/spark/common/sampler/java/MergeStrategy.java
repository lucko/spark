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

package me.lucko.spark.common.sampler.java;

import me.lucko.spark.common.sampler.node.StackTraceNode;
import me.lucko.spark.common.util.MethodDisambiguator;

import java.util.Objects;

/**
 * Strategy used to determine if {@link StackTraceNode}s should be merged.
 */
public enum MergeStrategy {

    SAME_METHOD(false),
    SEPARATE_PARENT_CALLS(true);

    private final boolean separateParentCalls;

    MergeStrategy(boolean separateParentCalls) {
        this.separateParentCalls = separateParentCalls;
    }

    public boolean separateParentCalls() {
        return this.separateParentCalls;
    }

    /**
     * Test if two stack trace nodes should be considered the same and merged.
     *
     * @param disambiguator the method disambiguator
     * @param n1 the first node
     * @param n2 the second node
     * @return if the nodes should be merged
     */
    public boolean shouldMerge(MethodDisambiguator disambiguator, StackTraceNode n1, StackTraceNode n2) {
        // are the class names the same?
        if (!n1.getClassName().equals(n2.getClassName())) {
            return false;
        }

        // are the method names the same?
        if (!n1.getMethodName().equals(n2.getMethodName())) {
            return false;
        }

        // is the parent line the same? (did the same line of code call this method?)
        if (this.separateParentCalls && n1.getParentLineNumber() != n2.getParentLineNumber()) {
            return false;
        }

        // are the method descriptions the same? (is it the same method?)
        String desc1 = disambiguator.disambiguate(n1).map(MethodDisambiguator.MethodDescription::getDescription).orElse(null);
        String desc2 = disambiguator.disambiguate(n2).map(MethodDisambiguator.MethodDescription::getDescription).orElse(null);

        if (desc1 == null && desc2 == null) {
            return true;
        }

        return Objects.equals(desc1, desc2);
    }

}
