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

package me.lucko.spark.common;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import me.lucko.spark.common.http.Bytebin;
import me.lucko.spark.memory.HeapDump;
import me.lucko.spark.monitor.TickMonitor;
import me.lucko.spark.sampler.Sampler;
import me.lucko.spark.sampler.SamplerBuilder;
import me.lucko.spark.sampler.ThreadDumper;
import me.lucko.spark.sampler.ThreadGrouper;
import me.lucko.spark.sampler.TickCounter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract command handling class used by all platforms.
 *
 * @param <T> the sender (e.g. CommandSender) type used by the platform
 */
public abstract class CommandHandler<T> {

    /** The URL of the viewer frontend */
    private static final String VIEWER_URL = "https://sparkprofiler.github.io/?";
    /** The prefix used in all messages */
    private static final String PREFIX = "&8[&fspark&8] &7";

    /** Guards {@link #activeSampler} */
    private final Object[] activeSamplerMutex = new Object[0];
    /** The WarmRoast instance currently running, if any */
    private Sampler activeSampler = null;
    /** The tick monitor instance currently running, if any */
    private ReportingTickMonitor activeTickMonitor = null;


    // abstract methods implemented by each platform

    protected abstract String getVersion();
    protected abstract String getLabel();
    protected abstract void sendMessage(T sender, String message);
    protected abstract void sendMessage(String message);
    protected abstract void sendLink(String url);
    protected abstract void runAsync(Runnable r);
    protected abstract ThreadDumper getDefaultThreadDumper();
    protected abstract TickCounter newTickCounter();

    private void sendPrefixedMessage(T sender, String message) {
        sendMessage(sender, PREFIX + message);
    }

    private void sendPrefixedMessage(String message) {
        sendMessage(PREFIX + message);
    }

    public void handleCommand(T sender, String[] args) {
        try {
            if (args.length == 0) {
                sendInfo(sender);
                return;
            }

            List<String> arguments = new ArrayList<>(Arrays.asList(args));
            switch (arguments.remove(0).toLowerCase()) {
                case "start":
                    handleStart(sender, arguments);
                    break;
                case "info":
                    handleInfo(sender);
                    break;
                case "cancel":
                    handleCancel(sender);
                    break;
                case "stop":
                case "upload":
                case "paste":
                    handleStop(sender);
                    break;
                case "monitoring":
                    handleMonitoring(sender, arguments);
                    break;
                case "heap":
                case "memory":
                    handleHeap(sender);
                    break;
                default:
                    sendInfo(sender);
                    break;
            }
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "&c" + e.getMessage());
        }
    }

    private void sendInfo(T sender) {
        sendPrefixedMessage(sender, "&fspark profiler &7v" + getVersion());
        sendMessage(sender, "&b&l> &7/" + getLabel() + " start");
        sendMessage(sender, "       &8[&7--timeout&8 <timeout seconds>]");
        sendMessage(sender, "       &8[&7--thread&8 <thread name>]");
        sendMessage(sender, "       &8[&7--not-combined]");
        sendMessage(sender, "       &8[&7--interval&8 <interval millis>]");
        sendMessage(sender, "       &8[&7--only-ticks-over&8 <tick length millis>]");
        sendMessage(sender, "&b&l> &7/" + getLabel() + " info");
        sendMessage(sender, "&b&l> &7/" + getLabel() + " stop");
        sendMessage(sender, "&b&l> &7/" + getLabel() + " cancel");
        sendMessage(sender, "&b&l> &7/" + getLabel() + " monitoring");
        sendMessage(sender, "       &8[&7--threshold&8 <percentage increase>]");
    }

    private void handleStart(T sender, List<String> args) {
        SetMultimap<String, String> arguments = parseArguments(args);

        int timeoutSeconds = parseInt(arguments, "timeout", "d");
        if (timeoutSeconds != -1 && timeoutSeconds <= 10) {
            sendPrefixedMessage(sender, "&cThe specified timeout is not long enough for accurate results to be formed. Please choose a value greater than 10.");
            return;
        }

        if (timeoutSeconds != -1 && timeoutSeconds < 30) {
            sendPrefixedMessage(sender, "&7The accuracy of the output will significantly improve when sampling is able to run for longer periods. Consider setting a timeout value over 30 seconds.");
        }

        int intervalMillis = parseInt(arguments, "interval", "i");
        if (intervalMillis <= 0) {
            intervalMillis = 4;
        }

        Set<String> threads = Sets.union(arguments.get("thread"), arguments.get("t"));
        ThreadDumper threadDumper;
        if (threads.isEmpty()) {
            // use the server thread
            threadDumper = getDefaultThreadDumper();
        } else if (threads.contains("*")) {
            threadDumper = ThreadDumper.ALL;
        } else {
            threadDumper = new ThreadDumper.Specific(threads);
        }

        ThreadGrouper threadGrouper;
        if (arguments.containsKey("not-combined")) {
            threadGrouper = ThreadGrouper.BY_NAME;
        } else {
            threadGrouper = ThreadGrouper.BY_POOL;
        }

        int ticksOver = parseInt(arguments, "only-ticks-over", "o");
        TickCounter tickCounter = null;
        if (ticksOver != -1) {
            try {
                tickCounter = newTickCounter();
            } catch (UnsupportedOperationException e) {
                sendPrefixedMessage(sender, "&cTick counting is not supported!");
                return;
            }
        }

        Sampler sampler;
        synchronized (this.activeSamplerMutex) {
            if (this.activeSampler != null) {
                sendPrefixedMessage(sender, "&7An active sampler is already running.");
                return;
            }

            sendPrefixedMessage("&7Initializing a new profiler, please wait...");

            SamplerBuilder builder = new SamplerBuilder();
            builder.threadDumper(threadDumper);
            builder.threadGrouper(threadGrouper);
            if (timeoutSeconds != -1) {
                builder.completeAfter(timeoutSeconds, TimeUnit.SECONDS);
            }
            builder.samplingInterval(intervalMillis);
            if (ticksOver != -1) {
                builder.ticksOver(ticksOver, tickCounter);
            }
            sampler = this.activeSampler = builder.start();

            sendPrefixedMessage("&bProfiler now active!");
            if (timeoutSeconds == -1) {
                sendPrefixedMessage("&7Use '/" + getLabel() + " stop' to stop profiling and upload the results.");
            } else {
                sendPrefixedMessage("&7The results will be automatically returned after the profiler has been running for " + timeoutSeconds + " seconds.");
            }
        }

        CompletableFuture<Sampler> future = sampler.getFuture();

        // send message if profiling fails
        future.whenCompleteAsync((s, throwable) -> {
            if (throwable != null) {
                sendPrefixedMessage("&cSampling operation failed unexpectedly. Error: " + throwable.toString());
                throwable.printStackTrace();
            }
        });

        // set activeSampler to null when complete.
        future.whenCompleteAsync((s, throwable) -> {
            synchronized (this.activeSamplerMutex) {
                if (sampler == this.activeSampler) {
                    this.activeSampler = null;
                }
            }
        });

        // await the result
        if (timeoutSeconds != -1) {
            future.thenAcceptAsync(s -> {
                sendPrefixedMessage("&7The active sampling operation has completed! Uploading results...");
                handleUpload(s);
            });
        }
    }

    private void handleInfo(T sender) {
        synchronized (this.activeSamplerMutex) {
            if (this.activeSampler == null) {
                sendPrefixedMessage(sender, "&7There isn't an active sampling task running.");
            } else {
                long timeout = this.activeSampler.getEndTime();
                if (timeout == -1) {
                    sendPrefixedMessage(sender, "&7There is an active sampler currently running, with no defined timeout.");
                } else {
                    long timeoutDiff = (timeout - System.currentTimeMillis()) / 1000L;
                    sendPrefixedMessage(sender, "&7There is an active sampler currently running, due to timeout in " + timeoutDiff + " seconds.");
                }

                long runningTime = (System.currentTimeMillis() - this.activeSampler.getStartTime()) / 1000L;
                sendPrefixedMessage(sender, "&7It has been sampling for " + runningTime + " seconds so far.");
            }
        }
    }

    private void handleStop(T sender) {
        synchronized (this.activeSamplerMutex) {
            if (this.activeSampler == null) {
                sendPrefixedMessage(sender, "&7There isn't an active sampling task running.");
            } else {
                this.activeSampler.cancel();
                sendPrefixedMessage("&7The active sampling operation has been stopped! Uploading results...");
                handleUpload(this.activeSampler);
                this.activeSampler = null;
            }
        }
    }

    private void handleCancel(T sender) {
        synchronized (this.activeSamplerMutex) {
            if (this.activeSampler == null) {
                sendPrefixedMessage(sender, "&7There isn't an active sampling task running.");
            } else {
                this.activeSampler.cancel();
                this.activeSampler = null;
                sendPrefixedMessage("&bThe active sampling task has been cancelled.");
            }
        }
    }

    private void handleUpload(Sampler sampler) {
        runAsync(() -> {
            byte[] output = sampler.formCompressedDataPayload();
            try {
                String pasteId = Bytebin.postCompressedContent(output);
                sendPrefixedMessage("&bSampling results:");
                sendLink(VIEWER_URL + pasteId);
            } catch (IOException e) {
                sendPrefixedMessage("&cAn error occurred whilst uploading the results.");
                e.printStackTrace();
            }
        });
    }

    private void handleMonitoring(T sender, List<String> args) {
        SetMultimap<String, String> arguments = parseArguments(args);

        if (this.activeTickMonitor == null) {

            int threshold = parseInt(arguments, "threshold", "t");
            if (threshold == -1) {
                threshold = 100;
            }

            try {
                TickCounter tickCounter = newTickCounter();
                this.activeTickMonitor = new ReportingTickMonitor(tickCounter, threshold);
            } catch (UnsupportedOperationException e) {
                sendPrefixedMessage(sender, "&cNot supported!");
            }
        } else {
            this.activeTickMonitor.close();
            this.activeTickMonitor = null;
            sendPrefixedMessage("&7Tick monitor disabled.");
        }
    }

    private void handleHeap(T sender) {
        runAsync(() -> {
            sendPrefixedMessage("&7Creating a new heap dump, please wait...");

            HeapDump heapDump;
            try {
                heapDump = HeapDump.createNew();
            } catch (Exception e) {
                sendPrefixedMessage("&cAn error occurred whilst inspecting the heap.");
                e.printStackTrace();
                return;
            }

            byte[] output = heapDump.formCompressedDataPayload();
            try {
                String pasteId = Bytebin.postCompressedContent(output);
                sendPrefixedMessage("&bHeap dump output:");
                sendLink(VIEWER_URL + pasteId);
            } catch (IOException e) {
                sendPrefixedMessage("&cAn error occurred whilst uploading the data.");
                e.printStackTrace();
            }
        });
    }

    private class ReportingTickMonitor extends TickMonitor {
        ReportingTickMonitor(TickCounter tickCounter, int percentageChangeThreshold) {
            super(tickCounter, percentageChangeThreshold);
        }

        @Override
        protected void sendMessage(String message) {
            sendPrefixedMessage(message);
        }
    }

    private int parseInt(SetMultimap<String, String> arguments, String longArg, String shortArg) {
        Iterator<String> it = Sets.union(arguments.get(longArg), arguments.get(shortArg)).iterator();
        if (it.hasNext()) {
            try {
                return Math.abs(Integer.parseInt(it.next()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid input for '" + longArg + "' argument. Please specify a number!");
            }
        }
        return -1; // undefined
    }

    private static final Pattern FLAG_REGEX = Pattern.compile("--(.+)$|-([a-zA-z])$");

    private static SetMultimap<String, String> parseArguments(List<String> args) {
        SetMultimap<String, String> arguments = HashMultimap.create();

        String flag = null;
        List<String> value = null;

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);

            Matcher matcher = FLAG_REGEX.matcher(arg);
            boolean matches = matcher.matches();

            if (flag == null || matches) {
                if (!matches) {
                    throw new IllegalArgumentException("Expected flag at position " + i + " but got '" + arg + "' instead!");
                }

                String match = matcher.group(1);
                if (match == null) {
                    match = matcher.group(2);
                }

                // store existing value, if present
                if (flag != null) {
                    arguments.put(flag, String.join(" ", value));
                }

                flag = match.toLowerCase();
                value = new ArrayList<>();
            } else {
                // part of a value
                value.add(arg);
            }
        }

        // store remaining value, if present
        if (flag != null) {
            arguments.put(flag, String.join(" ", value));
        }

        return arguments;
    }
    
}
