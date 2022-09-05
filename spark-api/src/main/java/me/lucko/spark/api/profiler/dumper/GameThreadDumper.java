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

package me.lucko.spark.api.profiler.dumper;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Utility to cache the creation of a {@link ThreadDumper} targeting
 * the game (server/client) thread.
 */
public final class GameThreadDumper implements Supplier<ThreadDumper> {
    private Supplier<Thread> threadSupplier;
    private SpecificThreadDumper dumper = null;

    public GameThreadDumper() {

    }

    public GameThreadDumper(Supplier<Thread> threadSupplier) {
        this.threadSupplier = threadSupplier;
    }

    @Override
    public ThreadDumper get() {
        if (this.dumper == null) {
            setThread(this.threadSupplier.get());
            this.threadSupplier = null;
        }

        return Objects.requireNonNull(this.dumper, "dumper");
    }

    public void setThread(Thread thread) {
        this.dumper = new SpecificThreadDumper(new long[]{thread.getId()});
    }
}
