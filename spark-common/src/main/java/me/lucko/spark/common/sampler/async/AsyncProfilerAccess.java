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

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.io.ByteStreams;
import me.lucko.spark.common.SparkPlatform;
import one.profiler.AsyncProfiler;
import one.profiler.Events;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Provides a bridge between spark and async-profiler.
 */
public class AsyncProfilerAccess {
    private static AsyncProfilerAccess instance;

    // singleton, needs a SparkPlatform for first init
    public static synchronized AsyncProfilerAccess getInstance(SparkPlatform platform) {
        if (instance == null) {
            Objects.requireNonNull(platform, "platform");
            instance = new AsyncProfilerAccess(platform);
        }
        return instance;
    }

    /** An instance of the async-profiler Java API. */
    private final AsyncProfiler profiler;

    /** The event to use for profiling */
    private final ProfilingEvent profilingEvent;
    /** The event to use for allocation profiling */
    private final ProfilingEvent allocationProfilingEvent;

    /** If profiler is null, contains the reason why setup failed */
    private final Exception setupException;

    AsyncProfilerAccess(SparkPlatform platform) {
        AsyncProfiler profiler;
        ProfilingEvent profilingEvent = null;
        ProfilingEvent allocationProfilingEvent = null;
        Exception setupException = null;

        try {
            profiler = load(platform);
            if (isEventSupported(profiler, ProfilingEvent.ALLOC, false)) {
                allocationProfilingEvent = ProfilingEvent.ALLOC;
            }
            if (isEventSupported(profiler, ProfilingEvent.WALL, true)) {
                profilingEvent = ProfilingEvent.WALL;
            }
        } catch (Exception e) {
            profiler = null;
            setupException = e;
        }

        this.profiler = profiler;
        this.profilingEvent = profilingEvent;
        this.allocationProfilingEvent = allocationProfilingEvent;
        this.setupException = setupException;
    }

    public AsyncProfilerJob startNewProfilerJob() {
        if (this.profiler == null) {
            throw new UnsupportedOperationException("async-profiler not supported", this.setupException);
        }
        return AsyncProfilerJob.createNew(this, this.profiler);
    }

    public ProfilingEvent getProfilingEvent() {
        return this.profilingEvent;
    }

    public ProfilingEvent getAllocationProfilingEvent() {
        return this.allocationProfilingEvent;
    }

    public boolean checkSupported(SparkPlatform platform) {
        if (this.setupException != null) {
            if (this.setupException instanceof UnsupportedSystemException) {
                platform.getPlugin().log(Level.INFO, "The async-profiler engine is not supported for your os/arch (" +
                        this.setupException.getMessage() + "), so the built-in Java engine will be used instead.");
            } else if (this.setupException instanceof UnsupportedJvmException) {
                platform.getPlugin().log(Level.INFO, "The async-profiler engine is not supported for your JVM (" +
                        this.setupException.getMessage() + "), so the built-in Java engine will be used instead.");
            } else if (this.setupException instanceof NativeLoadingException && this.setupException.getCause().getMessage().contains("libstdc++")) {
                platform.getPlugin().log(Level.WARNING, "Unable to initialise the async-profiler engine because libstdc++ is not installed.");
                platform.getPlugin().log(Level.WARNING, "Please see here for more information: https://spark.lucko.me/docs/misc/Using-async-profiler#install-libstdc");
            } else {
                String error = this.setupException.getMessage();
                if (this.setupException.getCause() != null) {
                    error += " (" + this.setupException.getCause().getMessage() + ")";
                }
                platform.getPlugin().log(Level.WARNING, "Unable to initialise the async-profiler engine: " + error);
                platform.getPlugin().log(Level.WARNING, "Please see here for more information: https://spark.lucko.me/docs/misc/Using-async-profiler");
            }

        }
        return this.profiler != null;
    }

    public boolean checkAllocationProfilingSupported(SparkPlatform platform) {
        boolean supported = this.allocationProfilingEvent != null;
        if (!supported && this.profiler != null) {
            platform.getPlugin().log(Level.WARNING, "The allocation profiling mode is not supported on your system. This is most likely because Hotspot debug symbols are not available.");
            platform.getPlugin().log(Level.WARNING, "To resolve, try installing the 'openjdk-11-dbg' or 'openjdk-8-dbg' package using your OS package manager.");
        }
        return supported;
    }

    public String getVersion() {
        return this.profiler.getVersion();
    }

    private static AsyncProfiler load(SparkPlatform platform) throws Exception {
        // check compatibility
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT).replace(" ", "");
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        String jvm = System.getProperty("java.vm.name");

        Table<String, String, String> supported = ImmutableTable.<String, String, String>builder()
                .put("linux", "amd64", "linux/amd64")
                .put("linux", "aarch64", "linux/aarch64")
                .put("macosx", "amd64", "macos")
                .put("macosx", "aarch64", "macos")
                .build();

        String libPath = supported.get(os, arch);
        if (libPath == null) {
            throw new UnsupportedSystemException(os, arch);
        }

        // extract the profiler binary from the spark jar file
        String resource = "spark-native/" + libPath + "/libasyncProfiler.so";
        URL profilerResource = AsyncProfilerAccess.class.getClassLoader().getResource(resource);
        if (profilerResource == null) {
            throw new IllegalStateException("Could not find " + resource + " in spark jar file");
        }

        Path extractPath = platform.getTemporaryFiles().create("spark-", "-libasyncProfiler.so.tmp");

        try (InputStream in = profilerResource.openStream(); OutputStream out = Files.newOutputStream(extractPath)) {
            ByteStreams.copy(in, out);
        }

        // get an instance of async-profiler
        try {
            return AsyncProfiler.getInstance(extractPath.toAbsolutePath().toString());
        } catch (UnsatisfiedLinkError e) {
            throw new NativeLoadingException(e);
        }
    }

    /**
     * Checks the {@code profiler} to ensure the CPU event is supported.
     *
     * @param profiler the profiler instance
     * @return if the event is supported
     */
    private static boolean isEventSupported(AsyncProfiler profiler, ProfilingEvent event, boolean throwException) {
        try {
            String resp = profiler.execute("check,event=" + event).trim();
            if (resp.equalsIgnoreCase("ok")) {
                return true;
            } else if (throwException) {
                throw new IllegalArgumentException(resp);
            }
        } catch (Exception e) {
            if (throwException) {
                throw new RuntimeException("Event " + event + " is not supported", e);
            }
        }
        return false;
    }

    public enum ProfilingEvent {
        WALL(Events.WALL),
        ALLOC(Events.ALLOC);

        private final String id;

        ProfilingEvent(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return this.id;
        }
    }

    private static final class UnsupportedSystemException extends UnsupportedOperationException {
        public UnsupportedSystemException(String os, String arch) {
            super(os + '/' + arch);
        }
    }

    private static final class UnsupportedJvmException extends UnsupportedOperationException {
        public UnsupportedJvmException(String jvm) {
            super(jvm);
        }
    }

    private static final class NativeLoadingException extends RuntimeException {
        public NativeLoadingException(Throwable cause) {
            super("A runtime error occurred whilst loading the native library", cause);
        }
    }
}
