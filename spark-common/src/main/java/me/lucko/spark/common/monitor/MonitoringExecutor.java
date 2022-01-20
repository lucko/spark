package me.lucko.spark.common.monitor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public enum MonitoringExecutor {
    ;

    /** The executor used to monitor & calculate rolling averages. */
    public static final ScheduledExecutorService INSTANCE = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setName("spark-monitor");
        thread.setDaemon(true);
        return thread;
    });
}
