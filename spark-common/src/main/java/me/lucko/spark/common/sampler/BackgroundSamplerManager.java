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

package me.lucko.spark.common.sampler;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.util.Configuration;

import java.util.logging.Level;

public class BackgroundSamplerManager {

    private static final String OPTION_ENABLED = "backgroundProfiler";
    private static final String OPTION_ENGINE = "backgroundProfilerEngine";
    private static final String OPTION_INTERVAL = "backgroundProfilerInterval";

    private static final String MARKER_FAILED = "_marker_background_profiler_failed";

    private final SparkPlatform platform;
    private final Configuration configuration;
    private final boolean enabled;

    public BackgroundSamplerManager(SparkPlatform platform, Configuration configuration) {
        this.platform = platform;
        this.configuration = configuration;
        this.enabled = this.configuration.getBoolean(
                OPTION_ENABLED,
                this.platform.getPlugin().getPlatformInfo().getType() == PlatformInfo.Type.SERVER
        );
    }

    public void initialise() {
        if (!this.enabled) {
            return;
        }

        // are we enabling the background profiler by default for the first time?
        boolean didEnableByDefault = false;
        if (!this.configuration.contains(OPTION_ENABLED)) {
            this.configuration.setBoolean(OPTION_ENABLED, true);
            didEnableByDefault = true;
        }

        // did the background profiler fail to start on the previous attempt?
        if (this.configuration.getBoolean(MARKER_FAILED, false)) {
            this.platform.getPlugin().log(Level.WARNING, "It seems the background profiler failed to start when spark was last enabled. Sorry about that!");
            this.platform.getPlugin().log(Level.WARNING, "In the future, spark will try to use the built-in Java profiling engine instead.");

            this.configuration.remove(MARKER_FAILED);
            this.configuration.setString(OPTION_ENGINE, "java");
            this.configuration.save();
        }

        this.platform.getPlugin().log(Level.INFO, "Starting background profiler...");

        if (didEnableByDefault) {
            // set the failed marker and save before we try to start the profiler,
            // then remove the marker afterwards if everything goes ok!
            this.configuration.setBoolean(MARKER_FAILED, true);
            this.configuration.save();
        }

        try {
            startSampler();

            if (didEnableByDefault) {
                this.configuration.remove(MARKER_FAILED);
                this.configuration.save();
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public boolean restartBackgroundSampler() {
        if (this.enabled) {
            startSampler();
            return true;
        }
        return false;
    }

    private void startSampler() {
        boolean forceJavaEngine = this.configuration.getString(OPTION_ENGINE, "async").equals("java");

        Sampler sampler = new SamplerBuilder()
                .background(true)
                .threadDumper(this.platform.getPlugin().getDefaultThreadDumper())
                .threadGrouper(ThreadGrouper.BY_POOL)
                .samplingInterval(this.configuration.getInteger(OPTION_INTERVAL, 10))
                .forceJavaSampler(forceJavaEngine)
                .start(this.platform);

        this.platform.getSamplerContainer().setActiveSampler(sampler);
    }

}
