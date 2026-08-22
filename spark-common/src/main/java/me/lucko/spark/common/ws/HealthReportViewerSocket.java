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

package me.lucko.spark.common.ws;

import me.lucko.bytesocks.client.BytesocksClient;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.monitor.Metrics;
import me.lucko.spark.common.util.SparkScheduledThreadPoolExecutor;
import me.lucko.spark.common.util.SparkThreadFactory;
import me.lucko.spark.proto.SparkProtos;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Represents a 'health report' connection with the spark viewer.
 */
public class HealthReportViewerSocket extends ViewerSocket {

    private final ScheduledExecutorService scheduler = new SparkScheduledThreadPoolExecutor(1, new SparkThreadFactory("spark-heath-report-socket-worker", false));

    public HealthReportViewerSocket(SparkPlatform platform, BytesocksClient client) throws Exception {
        super(platform, client);
        this.scheduler.scheduleAtFixedRate(this::tryTick, 10, 10, TimeUnit.SECONDS);
    }

    public void tryTick() {
        try {
            tick();
        } catch (Exception e) {
            this.platform.getPlugin().log(Level.WARNING, "Error whilst sending updated statistics to the socket", e);
        }
    }

    public void tick() {
        if (checkShouldClose()) {
            return;
        }

        SparkProtos.PlatformStatistics platform = this.platform.getStatisticsProvider().getPlatformStatistics(this.platform.getStartupGcStatistics(), false);
        SparkProtos.SystemStatistics system = this.platform.getStatisticsProvider().getSystemStatistics();
        SparkProtos.Metrics metrics = Metrics.exportProto();

        sendUpdatedStatistics(platform, system, metrics);
    }

    @Override
    public void close() {
        this.scheduler.shutdownNow();
        super.close();
    }
}
