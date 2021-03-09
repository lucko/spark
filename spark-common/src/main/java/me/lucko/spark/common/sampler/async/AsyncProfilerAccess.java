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

import one.profiler.AsyncProfiler;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Provides a bridge between spark and async-profiler.
 */
public final class AsyncProfilerAccess {

    // Singleton
    public static final AsyncProfilerAccess INSTANCE = new AsyncProfilerAccess();

    // Only support Linux x86_64
    private static final String SUPPORTED_OS = "linux";
    private static final String SUPPORTED_ARCH = "amd64";

    private static AsyncProfiler load() throws Exception {
        // check compatibility
        String os = System.getProperty("os.name");
        if (!SUPPORTED_OS.equalsIgnoreCase(os)) {
            throw new UnsupportedOperationException("Only supported on Linux x86_64, your OS: " + os);
        }

        String arch = System.getProperty("os.arch");
        if (!SUPPORTED_ARCH.equalsIgnoreCase(arch)) {
            throw new UnsupportedOperationException("Only supported on Linux x86_64, your arch: " + os);
        }

        // extract the profiler binary from the spark jar file
        URL profilerResource = AsyncProfilerAccess.class.getClassLoader().getResource("libasyncProfiler.so");
        if (profilerResource == null) {
            throw new IllegalStateException("Could not find libasyncProfiler.so in spark jar file");
        }

        Path extractPath = Files.createTempFile("spark-", "-libasyncProfiler.so.tmp");
        extractPath.toFile().deleteOnExit();

        try (InputStream in = profilerResource.openStream()) {
            Files.copy(in, extractPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // get an instance of async-profiler
        return AsyncProfiler.getInstance(extractPath.toAbsolutePath().toString());
    }

    /** An instance of the async-profiler Java API. */
    private final AsyncProfiler profiler;

    /** If profiler is null, contains the reason why setup failed */
    private final Exception setupException;

    private AsyncProfilerAccess() {
        AsyncProfiler profiler;
        Exception setupException = null;

        try {
            profiler = load();
            ensureCpuEventSupported(profiler);
        } catch (Exception e) {
            profiler = null;
            setupException = e;
        }

        this.profiler = profiler;
        this.setupException = setupException;
    }

    /**
     * Checks the {@code profiler} to ensure the CPU event is supported.
     *
     * @param profiler the profiler instance
     * @throws Exception if the event is not supported
     */
    private static void ensureCpuEventSupported(AsyncProfiler profiler) throws Exception {
        String resp = profiler.execute("check,event=cpu").trim();
        if (!resp.equalsIgnoreCase("ok")) {
            throw new UnsupportedOperationException("CPU event is not supported");
        }
    }

    public AsyncProfiler getProfiler() {
        if (this.profiler == null) {
            throw new UnsupportedOperationException("async-profiler not supported", this.setupException);
        }
        return this.profiler;
    }

    public boolean isSupported() {
        return this.profiler != null;
    }
}
