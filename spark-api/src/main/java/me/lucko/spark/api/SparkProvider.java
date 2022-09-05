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

package me.lucko.spark.api;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Singleton provider for {@link Spark}.
 */
@SuppressWarnings("unused")
public final class SparkProvider {

    private static final List<Consumer<Spark>> WHEN_LOADED = new CopyOnWriteArrayList<>();
    private static final List<Runnable> WHEN_UNLOADED = new CopyOnWriteArrayList<>();
    private static Spark instance;

    /**
     * Gets the singleton spark API instance.
     *
     * @return the spark API instance
     */
    public static @NonNull Spark get() {
        Spark instance = SparkProvider.instance;
        if (instance == null) {
            throw new IllegalStateException("spark has not loaded yet!");
        }
        return instance;
    }

    /**
     * Registers a listener called when spark is loaded.
     *
     * @param listener the listener
     */
    public static void whenLoaded(Consumer<Spark> listener) {
        WHEN_LOADED.add(listener);
    }

    /**
     * Registers a listener called when spark is unloaded.
     *
     * @param listener the listener
     */
    public static void whenUnloaded(Runnable listener) {
        WHEN_UNLOADED.add(listener);
    }

    static void set(Spark impl) {
        SparkProvider.instance = impl;
        // If null, we are unregistered
        if (impl == null) {
            WHEN_UNLOADED.forEach(Runnable::run);
        }
        // If non-null we are registered
        else {
            WHEN_LOADED.forEach(cons -> cons.accept(impl));
        }
    }

    private SparkProvider() {
        throw new AssertionError();
    }

}
