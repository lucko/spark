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
import me.lucko.spark.api.util.Sender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

public class ReportConfigurationBuilder {
    private Comparator<ThreadNode> order = ThreadOrder.BY_NAME;
    private Sender sender;
    private boolean separateParentCalls;
    private String comment;

    /**
     * Sets the order used by this builder.
     * @param order the order
     * @return the builder
     * @see ThreadOrder
     */
    public ReportConfigurationBuilder order(@NonNull Comparator<ThreadNode> order) {
        this.order = order;
        return this;
    }

    public ReportConfigurationBuilder sender(@Nullable Sender sender) {
        this.sender = sender;
        return this;
    }

    public ReportConfigurationBuilder sender(@NonNull String name) {
        return sender(new Sender(name, null));
    }

    public ReportConfigurationBuilder sender(@NonNull String name, @Nullable UUID uuid) {
        return sender(new Sender(name, uuid));
    }

    public ReportConfigurationBuilder separateParentCalls(boolean separateParentCalls) {
        this.separateParentCalls = separateParentCalls;
        return this;
    }

    public ReportConfigurationBuilder comment(@Nullable String comment) {
        this.comment = comment;
        return this;
    }

    public ReportConfiguration build() {
        return new ReportConfiguration() {
            @Override
            public Comparator<ThreadNode> getThreadOrder() {
                return order;
            }

            @Override
            public @Nullable Sender getSender() {
                return sender;
            }

            @Override
            public boolean separateParentCalls() {
                return separateParentCalls;
            }

            @Override
            public @Nullable String getComment() {
                return comment;
            }
        };
    }
}
