/*
 * This file is part of spark, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.spark.api.profiler.thread;

import java.util.Comparator;

/**
 * Methods of ordering {@link ThreadNode}s in the output data.
 */
public enum ThreadOrder implements Comparator<ThreadNode> {

    /**
     * Order by the name of the thread (alphabetically)
     */
    BY_NAME {
        @Override
        public int compare(ThreadNode o1, ThreadNode o2) {
            return o1.getLabel().compareTo(o2.getLabel());
        }
    },

    /**
     * Order by the time taken by the thread (most time taken first)
     */
    BY_TIME {
        @Override
        public int compare(ThreadNode o1, ThreadNode o2) {
            return -Double.compare(o1.getTotalTime(), o2.getTotalTime());
        }
    }
}
