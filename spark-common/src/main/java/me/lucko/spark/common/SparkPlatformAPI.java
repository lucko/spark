package me.lucko.spark.common;

import me.lucko.spark.common.activitylog.ActivityLog;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.sampler.tick.TickHook;
import me.lucko.spark.common.sampler.tick.TickReporter;

/**
 *
 */
public class SparkPlatformAPI {

    private final SparkPlatform platform;

    public SparkPlatformAPI(SparkPlatform platform) {
        this.platform = platform;
    }

    public ActivityLog getActivityLog() {
        return this.platform.getActivityLog();
    }

    public TickHook getTickHook() {
        return this.platform.getTickHook();
    }

    public TickReporter getTickReporter() {
        return this.platform.getTickReporter();
    }

    public TickStatistics getTickStatistics() {
        return this.platform.getTickStatistics();
    }
}
