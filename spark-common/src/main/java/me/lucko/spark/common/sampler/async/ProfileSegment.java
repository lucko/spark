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

package me.lucko.spark.common.sampler.async;

/**
 * Represents a profile "segment".
 *
 * <p>async-profiler groups unique stack traces together per-thread in its output.</p>
 */
public class ProfileSegment {

    /** The native thread id (does not correspond to Thread#getId) */
    private final int nativeThreadId;
    /** The name of the thread */
    private final String threadName;
    /** The stack trace for this segment */
    private final AsyncStackTraceElement[] stackTrace;
    /** The time spent executing this segment in microseconds */
    private final long time;

    public ProfileSegment(int nativeThreadId, String threadName, AsyncStackTraceElement[] stackTrace, long time) {
        this.nativeThreadId = nativeThreadId;
        this.threadName = threadName;
        this.stackTrace = stackTrace;
        this.time = time;
    }

    public int getNativeThreadId() {
        return this.nativeThreadId;
    }

    public String getThreadName() {
        return this.threadName;
    }

    public AsyncStackTraceElement[] getStackTrace() {
        return this.stackTrace;
    }

    public long getTime() {
        return this.time;
    }
}
