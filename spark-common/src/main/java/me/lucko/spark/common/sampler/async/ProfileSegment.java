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

import me.lucko.spark.common.sampler.async.jfr.JfrReader;

import java.nio.charset.StandardCharsets;

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
    private final long value;

    public ProfileSegment(int nativeThreadId, String threadName, AsyncStackTraceElement[] stackTrace, long value) {
        this.nativeThreadId = nativeThreadId;
        this.threadName = threadName;
        this.stackTrace = stackTrace;
        this.value = value;
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

    public long getValue() {
        return this.value;
    }

    public static ProfileSegment parseSegment(JfrReader reader, JfrReader.Event sample, String threadName, long value) {
        JfrReader.StackTrace stackTrace = reader.stackTraces.get(sample.stackTraceId);
        int len = stackTrace != null ? stackTrace.methods.length : 0;

        AsyncStackTraceElement[] stack = new AsyncStackTraceElement[len];
        for (int i = 0; i < len; i++) {
            stack[i] = parseStackFrame(reader, stackTrace.methods[i]);
        }

        return new ProfileSegment(sample.tid, threadName, stack, value);
    }

    private static AsyncStackTraceElement parseStackFrame(JfrReader reader, long methodId) {
        AsyncStackTraceElement result = reader.stackFrames.get(methodId);
        if (result != null) {
            return result;
        }

        JfrReader.MethodRef methodRef = reader.methods.get(methodId);
        JfrReader.ClassRef classRef = reader.classes.get(methodRef.cls);

        byte[] className = reader.symbols.get(classRef.name);
        byte[] methodName = reader.symbols.get(methodRef.name);

        if (className == null || className.length == 0) {
            // native call
            result = new AsyncStackTraceElement(
                    AsyncStackTraceElement.NATIVE_CALL,
                    new String(methodName, StandardCharsets.UTF_8),
                    null
            );
        } else {
            // java method
            byte[] methodDesc = reader.symbols.get(methodRef.sig);
            result = new AsyncStackTraceElement(
                    new String(className, StandardCharsets.UTF_8).replace('/', '.'),
                    new String(methodName, StandardCharsets.UTF_8),
                    new String(methodDesc, StandardCharsets.UTF_8)
            );
        }

        reader.stackFrames.put(methodId, result);
        return result;
    }

}
