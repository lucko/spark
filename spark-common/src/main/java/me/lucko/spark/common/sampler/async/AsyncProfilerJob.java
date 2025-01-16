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

import com.google.common.collect.ImmutableList;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.async.jfr.JfrReader;
import one.profiler.AsyncProfiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a profiling job within async-profiler.
 *
 * <p>Only one job can be running at a time. This is guarded by
 * {@link #createNew(AsyncProfilerAccess, AsyncProfiler)}.</p>
 */
public class AsyncProfilerJob {

    /**
     * The currently active job.
     */
    private static final AtomicReference<AsyncProfilerJob> ACTIVE = new AtomicReference<>();

    /**
     * Creates a new {@link AsyncProfilerJob}.
     *
     * <p>Will throw an {@link IllegalStateException} if another job is already active.</p>
     *
     * @param access the profiler access object
     * @param profiler the profiler
     * @return the job
     */
    static AsyncProfilerJob createNew(AsyncProfilerAccess access, AsyncProfiler profiler) {
        synchronized (ACTIVE) {
            AsyncProfilerJob existing = ACTIVE.get();
            if (existing != null) {
                throw new IllegalStateException("Another profiler is already active: " + existing);
            }

            AsyncProfilerJob job = new AsyncProfilerJob(access, profiler);
            ACTIVE.set(job);
            return job;
        }
    }

    /** The async-profiler access object */
    private final AsyncProfilerAccess access;
    /** The async-profiler instance */
    private final AsyncProfiler profiler;

    // Set on init
    /** The platform */
    private SparkPlatform platform;
    /** The sample collector */
    private SampleCollector<?> sampleCollector;
    /** The thread dumper */
    private ThreadDumper threadDumper;
    /** The profiling window */
    private int window;
    /** If the profiler should run in quiet mode */
    private boolean quiet;
    /** If the profiler needs to use the same clock as {@link System#nanoTime()} */
    private boolean forceNanoTime;

    /** The file used by async-profiler to output data */
    private Path outputFile;

    private AsyncProfilerJob(AsyncProfilerAccess access, AsyncProfiler profiler) {
        this.access = access;
        this.profiler = profiler;
    }

    /**
     * Executes an async-profiler command.
     *
     * @param command the command
     * @return the output
     */
    private String execute(Collection<String> command) {
        try {
            return this.profiler.execute(String.join(",", command));
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst executing profiler command", e);
        }
    }

    /**
     * Checks to ensure that this job is still active.
     */
    private void checkActive() {
        if (ACTIVE.get() != this) {
            throw new IllegalStateException("Profiler job no longer active!");
        }
    }

    // Initialise the job
    public void init(SparkPlatform platform, SampleCollector<?> collector, ThreadDumper threadDumper, int window, boolean quiet, boolean forceNanoTime) {
        this.platform = platform;
        this.sampleCollector = collector;
        this.threadDumper = threadDumper;
        this.window = window;
        this.quiet = quiet;
        this.forceNanoTime = forceNanoTime;
    }

    /**
     * Starts the job.
     */
    public void start() {
        checkActive();

        try {
            // create a new temporary output file
            try {
                this.outputFile = this.platform.getTemporaryFiles().create("spark-", "-profile-data.jfr.tmp");
            } catch (IOException e) {
                throw new RuntimeException("Unable to create temporary output file", e);
            }

            // construct a command to send to async-profiler
            ImmutableList.Builder<String> command = ImmutableList.<String>builder()
                    .add("start")
                    .addAll(this.sampleCollector.initArguments(this.access))
                    .add("threads").add("jfr").add("file=" + this.outputFile.toString());

            if (this.quiet) {
                command.add("loglevel=NONE");
            }
            if (this.threadDumper instanceof ThreadDumper.Specific) {
                command.add("filter");
            }
            if (this.forceNanoTime) {
                command.add("clock=monotonic");
            }

            // start the profiler
            String resp = execute(command.build()).trim();

            if (!resp.equalsIgnoreCase("profiling started")) {
                throw new RuntimeException("Unexpected response: " + resp);
            }

            // append threads to be profiled, if necessary
            if (this.threadDumper instanceof ThreadDumper.Specific) {
                ThreadDumper.Specific threadDumper = (ThreadDumper.Specific) this.threadDumper;
                for (Thread thread : threadDumper.getThreads()) {
                    this.profiler.addThread(thread);
                }
            }

        } catch (Exception e) {
            try {
                this.profiler.stop();
            } catch (Exception e2) {
                // ignore
            }
            close();

            throw e;
        }
    }

    /**
     * Stops the job.
     */
    public void stop() {
        checkActive();

        try {
            this.profiler.stop();
        } catch (IllegalStateException e) {
            if (!e.getMessage().equals("Profiler is not active")) { // ignore
                throw e;
            }
        } finally {
            close();
        }
    }

    /**
     * Aggregates the collected data.
     */
    public void aggregate(AsyncDataAggregator dataAggregator) {
        // read the jfr file produced by async-profiler
        try (JfrReader reader = new JfrReader(this.outputFile)) {
            readSegments(reader, this.sampleCollector, dataAggregator);
        } catch (Exception e) {
            boolean fileExists;
            try {
                fileExists = Files.exists(this.outputFile) && Files.size(this.outputFile) != 0;
            } catch (IOException ex) {
                fileExists = false;
            }

            if (fileExists) {
                throw new JfrParsingException("Error parsing JFR data from profiler output", e);
            } else {
                throw new JfrParsingException("Error parsing JFR data from profiler output - file " + this.outputFile + " does not exist!", e);
            }
        }

        deleteOutputFile();
    }

    public void deleteOutputFile() {
        try {
            Files.deleteIfExists(this.outputFile);
        } catch (IOException e) {
            // ignore
        }
    }

    private <E extends JfrReader.Event> void readSegments(JfrReader reader, SampleCollector<E> collector, AsyncDataAggregator dataAggregator) throws IOException {
        List<E> samples = reader.readAllEvents(collector.eventClass());
        for (E sample : samples) {
            String threadName = reader.threads.get((long) sample.tid);
            if (threadName == null) {
                continue;
            }

            if (!this.threadDumper.isThreadIncluded(sample.tid, threadName)) {
                continue;
            }

            long value = collector.measure(sample);

            // parse the segment and give it to the data aggregator
            ProfileSegment segment = ProfileSegment.parseSegment(reader, sample, threadName, value);
            dataAggregator.insertData(segment, this.window);
        }
    }

    public int getWindow() {
        return this.window;
    }

    private void close() {
        ACTIVE.compareAndSet(this, null);
    }
}
