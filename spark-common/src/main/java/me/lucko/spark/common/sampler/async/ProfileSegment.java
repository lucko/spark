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

import com.google.common.collect.ImmutableMap;
import me.lucko.spark.common.sampler.async.jfr.JfrReader;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Represents a profile "segment".
 *
 * <p>async-profiler groups unique stack traces together per-thread in its output.</p>
 */
public class ProfileSegment {

    private static final String UNKNOWN_THREAD_STATE = "<unknown>";

    /** The native thread id (does not correspond to Thread#getId) */
    private final int nativeThreadId;
    /** The name of the thread */
    private final String threadName;
    /** The stack trace for this segment */
    private final AsyncStackTraceElement[] stackTrace;
    /** The time spent executing this segment in microseconds */
    private final long value;
    /** The state of the thread. {@value #UNKNOWN_THREAD_STATE} if state is unknown */
    private final String threadState;
    /** The time at which this segment was recorded, as if it was produced by {@link System#nanoTime()} */
    private final long time;

    private ProfileSegment(int nativeThreadId, String threadName, AsyncStackTraceElement[] stackTrace, long value, String threadState, long time) {
        this.nativeThreadId = nativeThreadId;
        this.threadName = threadName;
        this.stackTrace = stackTrace;
        this.value = value;
        this.threadState = threadState;
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

    public long getValue() {
        return this.value;
    }

    public String getThreadState() {
        return this.threadState;
    }

    public long getTime() {
        return this.time;
    }

    public static ProfileSegment parseSegment(JfrReader reader, JfrReader.Event sample, String threadName, long value) {
        JfrReader.StackTrace stackTrace = reader.stackTraces.get(sample.stackTraceId);
        int len = stackTrace != null ? stackTrace.methods.length : 0;

        AsyncStackTraceElement[] stack = new AsyncStackTraceElement[len];
        for (int i = 0; i < len; i++) {
            stack[i] = parseStackFrame(reader, stackTrace.methods[i]);
        }
        String threadState = UNKNOWN_THREAD_STATE;
        if (sample instanceof JfrReader.ExecutionSample) {
            JfrReader.ExecutionSample executionSample = (JfrReader.ExecutionSample) sample;

            Map<Integer, String> threadStateLookup = reader.enums.getOrDefault("jdk.types.ThreadState", ImmutableMap.of());
            threadState = threadStateLookup.getOrDefault(executionSample.threadState, UNKNOWN_THREAD_STATE);
        }

        return new ProfileSegment(sample.tid, threadName, stack, value, threadState, sample.time);
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
