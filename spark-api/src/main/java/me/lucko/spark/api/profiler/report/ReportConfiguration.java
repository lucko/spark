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

package me.lucko.spark.api.profiler.report;

import me.lucko.spark.api.profiler.thread.ThreadNode;
import me.lucko.spark.api.profiler.thread.ThreadOrder;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

/**
 * Configuration for {@link ProfilerReport reports}.
 */
public interface ReportConfiguration {
    static ReportConfigurationBuilder builder() {
        return new ReportConfigurationBuilder();
    }

    /**
     * Gets the ordering used by the report.
     *
     * @return the ordering used by the report
     * @see ThreadOrder
     */
    Comparator<ThreadNode> threadOrder();

    /**
     * Gets the sender of the report
     *
     * @return the report's sender, or else {@code null}
     */
    @Nullable
    Sender sender();

    /**
     * If the thread viewer should separate parent calls.
     *
     * @return if the thread viewer should separate parent calls
     */
    boolean separateParentCalls();

    /**
     * Gets the comment of the report.
     *
     * @return the report's comment
     */
    @Nullable
    String comment();

    class Sender {
        public final String name;
        public final UUID uuid;

        public Sender(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }
}
