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

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.Sampler;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.proto.SparkProtos;

import one.profiler.AsyncProfiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A sampler implementation using async-profiler.
 */
public class AsyncSampler implements Sampler {
    private final AsyncProfiler profiler;

    /** The instance used to generate thread information for use in sampling */
    private final ThreadDumper threadDumper;
    /** Responsible for aggregating and then outputting collected sampling data */
    private final AsyncDataAggregator dataAggregator;
    /** Flag to mark if the output has been completed */
    private boolean outputComplete = false;

    /** The interval to wait between sampling, in microseconds */
    private final int interval;
    /** The time when sampling first began */
    private long startTime = -1;

    public AsyncSampler(int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper) {
        this.profiler = AsyncProfilerAccess.INSTANCE.getProfiler();
        this.threadDumper = threadDumper;
        this.dataAggregator = new AsyncDataAggregator(threadGrouper);
        this.interval = interval;
    }

    @Override
    public long getStartTime() {
        if (this.startTime == -1) {
            throw new IllegalStateException("Not yet started");
        }
        return this.startTime;
    }

    @Override
    public long getEndTime() {
        return -1;
    }

    @Override
    public CompletableFuture<? extends Sampler> getFuture() {
        return new CompletableFuture<>();
    }

    /**
     * Executes a profiler command.
     *
     * @param command the command to execute
     * @return the response
     */
    private String execute(String command) {
        try {
            return this.profiler.execute(command);
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst executing profiler command", e);
        }
    }

    /**
     * Starts the profiler.
     */
    @Override
    public void start() {
        this.startTime = System.currentTimeMillis();

        String command = "start,event=cpu,interval=" + this.interval + "us,threads";
        if (this.threadDumper instanceof ThreadDumper.Specific) {
            command += ",filter";
        }

        String resp = execute(command).trim();
        if (!resp.equalsIgnoreCase("profiling started")) {
            throw new RuntimeException("Unexpected response: " + resp);
        }

        if (this.threadDumper instanceof ThreadDumper.Specific) {
            ThreadDumper.Specific threadDumper = (ThreadDumper.Specific) this.threadDumper;
            for (Thread thread : threadDumper.getThreads()) {
                this.profiler.addThread(thread);
            }
        }
    }

    /**
     * Stops the profiler.
     */
    @Override
    public void stop() {
        this.profiler.stop();
    }

    @Override
    public SparkProtos.SamplerData toProto(PlatformInfo platformInfo, CommandSender creator, Comparator<? super Map.Entry<String, ThreadNode>> outputOrder, String comment, MergeMode mergeMode) {
        final SparkProtos.SamplerMetadata.Builder metadata = SparkProtos.SamplerMetadata.newBuilder()
                .setPlatform(platformInfo.toData().toProto())
                .setUser(creator.toData().toProto())
                .setStartTime(this.startTime)
                .setInterval(this.interval)
                .setThreadDumper(this.threadDumper.getMetadata())
                .setDataAggregator(this.dataAggregator.getMetadata());

        if (comment != null) {
            metadata.setComment(comment);
        }

        SparkProtos.SamplerData.Builder proto = SparkProtos.SamplerData.newBuilder();
        proto.setMetadata(metadata.build());

        aggregateOutput();

        List<Map.Entry<String, ThreadNode>> data = new ArrayList<>(this.dataAggregator.getData().entrySet());
        data.sort(outputOrder);

        for (Map.Entry<String, ThreadNode> entry : data) {
            proto.addThreads(entry.getValue().toProto(mergeMode));
        }

        return proto.build();
    }

    private void aggregateOutput() {
        if (this.outputComplete) {
            return;
        }
        this.outputComplete = true;

        String response = execute("traces,sig,ann");
        Splitter splitter = Splitter.on('\n');
        PeekingIterator<String> iterator = Iterators.peekingIterator(splitter.split(response).iterator());

        // skip over execution profile header
        expectLine(iterator, s -> s.contains("Execution profile"));
        while (!iterator.peek().startsWith("---")) {
            iterator.next();
        }

        Predicate<String> threadFilter;
        if (this.threadDumper instanceof ThreadDumper.Specific) {
            ThreadDumper.Specific threadDumper = (ThreadDumper.Specific) this.threadDumper;
            threadFilter = n -> threadDumper.getThreadNames().contains(n.toLowerCase());
        } else {
            threadFilter = n -> true;
        }

        // Read elements from the iterator until EOF
        while (iterator.hasNext() && !iterator.peek().trim().isEmpty()) {
            ProfileSegment elem = parseSegment(iterator);
            if (elem != null && threadFilter.apply(elem.getThreadName())) {
                this.dataAggregator.insertData(elem);
            }

            if (iterator.hasNext()) {
                iterator.next();
            }
        }
    }

    // Matches the segment header
    // e.g. '--- 13127066990 ns (11.47%), 3220 samples'
    private static final Pattern SEGMENT_HEADER = Pattern.compile("^--- (\\d+) ns \\(.+%\\), \\d+ samples?$");

    // Matches a stack entry enclosed under an segment
    // e.g. '  [ 1] jdk.internal.misc.Unsafe.park(ZJ)V_[j]'
    private static final Pattern SEGMENT_STACK_ENTRY = Pattern.compile("^ {2}\\[ *\\d+] (.+)$");

    // Matches a thread label footer (the last stack element in a segment)
    // e.g. '  [10] [Server thread tid=2504]'
    private static final Pattern SEGMENT_THREAD_LABEL = Pattern.compile("^ {2}\\[ *\\d+] \\[(.+) tid=(\\d+)]$");

    // Naively matches a Java stack element
    // e.g. 'jdk.internal.misc.Unsafe.park(ZJ)V_[j]'
    private static final Pattern NAIVE_JAVA_STACK_ELEMENT = Pattern.compile("^(.*)\\.(.*)(\\(.*)_\\[j]$");

    /**
     * Parse a single profiled segment from the iterator
     *
     * @param iterator the iterator to read from
     * @return the parsed element
     */
    private static ProfileSegment parseSegment(PeekingIterator<String> iterator) {
        // read & parse the header line
        String header = iterator.next();
        Matcher matcher = SEGMENT_HEADER.matcher(header);
        if (!matcher.matches()) {
            throw new IllegalStateException("Unable to read header: " + header);
        }
        long time = TimeUnit.NANOSECONDS.toMicros(Long.parseLong(matcher.group(1)));

        // parse the element entries.
        // elements from 0 to n-1 are part of thr stack
        // the element at n (the end) is a footer to denote the id/name of the thread
        List<AsyncStackTraceElement> stack = new ArrayList<>();
        String threadName;
        int threadId;

        while (true) {
            // attempt to parse the thread id/name footer
            // if found, break from the loop
            Matcher footerMatcher = SEGMENT_THREAD_LABEL.matcher(iterator.peek());
            if (footerMatcher.matches()) {
                threadName = footerMatcher.group(1);
                threadId = Integer.parseInt(footerMatcher.group(2));
                iterator.next();
                break;
            }

            // otherwise, try to parse a stack entry
            Matcher stackEntryMatcher = SEGMENT_STACK_ENTRY.matcher(iterator.peek());
            if (!stackEntryMatcher.matches()) {
                if (iterator.peek().trim().isEmpty()) {
                    return null;
                }
                throw new IllegalStateException("Unable to read stack entry: " + iterator.peek());
            }
            stack.add(readStackElement(stackEntryMatcher.group(1)));
            iterator.next();
        }

        return new ProfileSegment(
                threadId,
                threadName,
                stack.toArray(new AsyncStackTraceElement[0]),
                time
        );
    }

    /**
     * Parse a stack trace element from the given string
     *
     * @param element the string
     * @return the parsed stack track element
     */
    private static AsyncStackTraceElement readStackElement(String element) {
        Matcher matcher = NAIVE_JAVA_STACK_ELEMENT.matcher(element);
        if (matcher.matches()) {
            return new AsyncStackTraceElement(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3)
            );
        } else {
            return new AsyncStackTraceElement("native", element, null);
        }
    }

    private static void expectLine(Iterator<String> it, Predicate<String> test) {
        if (!it.hasNext()) {
            throw new IllegalStateException("Unexpected EOF");
        }
        String line = it.next();
        if (!test.apply(line)) {
            throw new IllegalStateException("Unexpected line");
        }
    }
}
