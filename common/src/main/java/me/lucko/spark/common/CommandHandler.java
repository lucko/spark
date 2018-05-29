package me.lucko.spark.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;

import me.lucko.spark.common.http.Bytebin;
import me.lucko.spark.profiler.Sampler;
import me.lucko.spark.profiler.SamplerBuilder;
import me.lucko.spark.profiler.ThreadDumper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Abstract command handling class used by all platforms.
 *
 * @param <T> the sender (e.g. CommandSender) type used by the platform
 */
public abstract class CommandHandler<T> {

    /** The URL of the viewer frontend */
    private static final String VIEWER_URL = "https://sparkprofiler.github.io/?";

    /**
     * The {@link Timer} being used by the {@link #activeSampler}.
     */
    private final Timer samplingThread = new Timer("spark-sampling-thread", true);

    /**
     * The worker {@link ExecutorService} being used by the {@link #activeSampler}.
     */
    private final ExecutorService workerPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("spark-worker-%d").build());

    /** Guards {@link #activeSampler} */
    private final Object[] activeSamplerMutex = new Object[0];
    /** The WarmRoast instance currently running, if any */
    private Sampler activeSampler = null;


    // abstract methods implemented by each platform

    protected abstract void sendMessage(T sender, String message);
    protected abstract void sendLink(T sender, String url);
    protected abstract void runAsync(Runnable r);

    private void sendPrefixedMessage(T sender, String message) {
        sendMessage(sender, "&8[&fspark&8] &7" + message);
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
                default:
                    sendInfo(sender);
                    break;
            }
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "&c" + e.getMessage());
        }
    }

    private void sendInfo(T sender) {
        sendPrefixedMessage(sender, "&fspark profiler &7v1.0");
        sendMessage(sender, "&b&l> &7/profiler start");
        sendMessage(sender, "       &8[&7--timeout&8 <timeout seconds>]");
        sendMessage(sender, "       &8[&7--thread&8 <thread name>]");
        sendMessage(sender, "       &8[&7--interval&8 <interval millis>]");
        sendMessage(sender, "&b&l> &7/profiler info");
        sendMessage(sender, "&b&l> &7/profiler stop");
        sendMessage(sender, "&b&l> &7/profiler cancel");
    }

    private void handleStart(T sender, List<String> args) {
        Map<String, String> arguments = parseArguments(args);

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
            intervalMillis = 10;
        }

        String threadName = arguments.getOrDefault("thread", arguments.getOrDefault("t", null));
        ThreadDumper threadDumper;
        if (threadName == null) {
            // use the server thread
            threadDumper = new ThreadDumper.Specific(new long[]{Thread.currentThread().getId()});
        } else if (threadName.equals("*")) {
            threadDumper = new ThreadDumper.All();
        } else {
            threadDumper = new ThreadDumper.Specific(threadName);
        }

        Sampler sampler;
        synchronized (this.activeSamplerMutex) {
            if (this.activeSampler != null) {
                sendPrefixedMessage(sender, "&7An active sampler is already running.");
                return;
            }

            sendPrefixedMessage(sender, "&7Initializing a new profiler, please wait...");

            SamplerBuilder builder = new SamplerBuilder();
            builder.threadDumper(threadDumper);
            if (timeoutSeconds != -1) {
                builder.completeAfter(timeoutSeconds, TimeUnit.SECONDS);
            }
            builder.samplingInterval(intervalMillis);
            sampler = this.activeSampler = builder.start(this.samplingThread, this.workerPool);

            sendPrefixedMessage(sender, "&bProfiler now active!");
            if (timeoutSeconds == -1) {
                sendPrefixedMessage(sender, "&7Use '/profiler stop' to stop profiling and upload the results.");
            } else {
                sendPrefixedMessage(sender, "&7The results will be automatically returned after the profiler has been running for " + timeoutSeconds + " seconds.");
            }
        }

        CompletableFuture<Sampler> future = sampler.getFuture();

        // send message if profiling fails
        future.whenCompleteAsync((s, throwable) -> {
            if (throwable != null) {
                sendPrefixedMessage(sender, "&cSampling operation failed unexpectedly. Error: " + throwable.toString());
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
                sendPrefixedMessage(sender, "&7The active sampling operation has completed! Uploading results...");
                handleUpload(sender, s);
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
                sendPrefixedMessage(sender, "&7The active sampling operation has been stopped! Uploading results...");
                handleUpload(sender, this.activeSampler);
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
                sendPrefixedMessage(sender, "&bThe active sampling task has been cancelled.");
            }
        }
    }

    private void handleUpload(T sender, Sampler sampler) {
        runAsync(() -> {
            JsonObject output = sampler.formOutput();
            try {
                String pasteId = Bytebin.postContent(output);
                sendPrefixedMessage(sender, "&bSampling results:");
                sendLink(sender, VIEWER_URL + pasteId);
            } catch (IOException e) {
                sendPrefixedMessage(sender, "&cAn error occurred whilst uploading the results.");
                e.printStackTrace();
            }
        });
    }

    private int parseInt(Map<String, String> arguments, String longArg, String shortArg) {
        String value = arguments.getOrDefault(longArg, arguments.getOrDefault(shortArg, null));
        if (value != null) {
            try {
                return Math.abs(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid input for '" + longArg + "' argument. Please specify a number!");
            }
        } else {
            return -1; // undefined
        }
    }

    private static final Pattern FLAG_REGEX = Pattern.compile("--(.+)$|-([a-zA-z])$");

    private static Map<String, String> parseArguments(List<String> args) {
        Map<String, String> arguments = new HashMap<>();

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
                    arguments.put(flag, value.stream().collect(Collectors.joining(" ")));
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
            arguments.put(flag, value.stream().collect(Collectors.joining(" ")));
        }

        return arguments;
    }
    
}
