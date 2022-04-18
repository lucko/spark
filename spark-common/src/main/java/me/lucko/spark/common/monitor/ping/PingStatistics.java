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

package me.lucko.spark.common.monitor.ping;

import me.lucko.spark.common.monitor.MonitoringExecutor;
import me.lucko.spark.common.util.RollingAverage;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Provides statistics for player ping RTT to the server.
 */
public final class PingStatistics implements Runnable, AutoCloseable {
    private static final int QUERY_RATE_SECONDS = 10;
    private static final int WINDOW_SIZE_SECONDS = (int) TimeUnit.MINUTES.toSeconds(15); // 900
    private static final int WINDOW_SIZE = WINDOW_SIZE_SECONDS / QUERY_RATE_SECONDS; // 90

    /** The platform function that provides player ping times */
    private final PlayerPingProvider provider;
    /** Rolling average of the median ping across all players */
    private final RollingAverage rollingAverage = new RollingAverage(WINDOW_SIZE);

    /** The scheduler task that polls pings and calculates the rolling average */
    private ScheduledFuture<?> future;

    public PingStatistics(PlayerPingProvider provider) {
        this.provider = provider;
    }

    /**
     * Starts the statistics monitor
     */
    public void start() {
        if (this.future != null) {
            throw new IllegalStateException();
        }
        this.future = MonitoringExecutor.INSTANCE.scheduleAtFixedRate(this, QUERY_RATE_SECONDS, QUERY_RATE_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        if (this.future != null) {
            this.future.cancel(false);
            this.future = null;
        }
    }

    @Override
    public void run() {
        PingSummary summary = currentSummary();
        if (summary.total() == 0) {
            return;
        }

        this.rollingAverage.add(BigDecimal.valueOf(summary.median()));
    }

    /**
     * Gets the ping rolling average.
     *
     * @return the rolling average
     */
    public RollingAverage getPingAverage() {
        return this.rollingAverage;
    }

    /**
     * Queries a summary of current player pings.
     *
     * @return a summary of current pings
     */
    public PingSummary currentSummary() {
        Map<String, Integer> results = this.provider.poll();
        int[] values = results.values().stream().filter(ping -> ping > 0).mapToInt(i -> i).toArray();
        return values.length == 0
                ? new PingSummary(new int[]{0})
                : new PingSummary(values);
    }

    /**
     * Queries the ping of a given player.
     *
     * @param playerName the name of the player
     * @return the ping, if available
     */
    public @Nullable PlayerPing query(String playerName) {
        Map<String, Integer> results = this.provider.poll();

        // try exact match
        Integer result = results.get(playerName);
        if (result != null) {
            return new PlayerPing(playerName, result);
        }

        // try case-insensitive match
        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(playerName)) {
                return new PlayerPing(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }

        return null;
    }

    public static final class PlayerPing {
        private final String name;
        private final int ping;

        PlayerPing(String name, int ping) {
            this.name = name;
            this.ping = ping;
        }

        public String name() {
            return this.name;
        }

        public int ping() {
            return this.ping;
        }
    }

}
