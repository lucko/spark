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

package me.lucko.spark.api.heap;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.util.Sender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Utility interface used for heap analysis.
 *
 * @see Spark#heapAnalysis()
 */
public interface HeapAnalysis {

    /**
     * Creates a summary of the heap.
     *
     * @param sender the sender of the report
     * @return the report
     */
    @NotNull
    HeapSummaryReport summary(@Nullable Sender sender);

    /**
     * Creates a heap dump at the given output path.
     *
     * @param outputPath the path to write the snapshot to
     * @param liveOnly   if true dump only live objects i.e. objects that are reachable from others
     */
    @NotNull
    @CanIgnoreReturnValue
    Path dumpHeap(Path outputPath, boolean liveOnly) throws Exception;
}
