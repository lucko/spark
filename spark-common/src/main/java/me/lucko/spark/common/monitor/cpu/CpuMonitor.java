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

package me.lucko.spark.common.monitor.cpu;

import me.lucko.spark.common.monitor.Metrics;
import me.lucko.spark.common.monitor.MonitoringExecutor;
import me.lucko.spark.common.util.RollingAverage;
import me.lucko.spark.common.util.TimeUtil;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;

/**
 * Exposes and monitors the system/process CPU usage.
 */
public enum CpuMonitor {
    ;

    /** The object name of the com.sun.management.OperatingSystemMXBean */
    private static final String OPERATING_SYSTEM_BEAN = "java.lang:type=OperatingSystem";
    /** The OperatingSystemMXBean instance */
    private static final OperatingSystemMXBean BEAN;

    // Rolling averages for system/process data
    private static final RollingAverage SYSTEM_AVERAGE_10_SEC = new RollingAverage(10);
    private static final RollingAverage SYSTEM_AVERAGE_1_MIN = new RollingAverage(60);
    private static final RollingAverage SYSTEM_AVERAGE_15_MIN = new RollingAverage(60 * 15);
    private static final RollingAverage PROCESS_AVERAGE_10_SEC = new RollingAverage(10);
    private static final RollingAverage PROCESS_AVERAGE_1_MIN = new RollingAverage(60);
    private static final RollingAverage PROCESS_AVERAGE_15_MIN = new RollingAverage(60 * 15);

    static {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName diagnosticBeanName = ObjectName.getInstance(OPERATING_SYSTEM_BEAN);
            BEAN = JMX.newMXBeanProxy(beanServer, diagnosticBeanName, OperatingSystemMXBean.class);
        } catch (Exception e) {
            throw new UnsupportedOperationException("OperatingSystemMXBean is not supported by the system", e);
        }

        MonitoringExecutor.scheduleAtFixedRateMillis(new PollingTask(), 1000 /* 1 second */);
    }

    /**
     * Ensures that the static initializer has been called.
     */
    @SuppressWarnings("EmptyMethod")
    public static void ensureMonitoring() {
        // intentionally empty
    }

    /**
     * Returns the "recent cpu usage" for the whole system. This value is a
     * double in the [0.0,1.0] interval. A value of 0.0 means that all CPUs
     * were idle during the recent period of time observed, while a value
     * of 1.0 means that all CPUs were actively running 100% of the time
     * during the recent period being observed. All values betweens 0.0 and
     * 1.0 are possible depending of the activities going on in the system.
     * If the system recent cpu usage is not available, the method returns a
     * negative value.
     *
     * @return the "recent cpu usage" for the whole system; a negative
     * value if not available.
     */
    public static double systemLoad() {
        return BEAN.getSystemCpuLoad();
    }

    public static double systemLoad10SecAvg() {
        return SYSTEM_AVERAGE_10_SEC.mean();
    }

    public static double systemLoad1MinAvg() {
        return SYSTEM_AVERAGE_1_MIN.mean();
    }

    public static double systemLoad15MinAvg() {
        return SYSTEM_AVERAGE_15_MIN.mean();
    }

    /**
     * Returns the "recent cpu usage" for the Java Virtual Machine process.
     * This value is a double in the [0.0,1.0] interval. A value of 0.0 means
     * that none of the CPUs were running threads from the JVM process during
     * the recent period of time observed, while a value of 1.0 means that all
     * CPUs were actively running threads from the JVM 100% of the time
     * during the recent period being observed. Threads from the JVM include
     * the application threads as well as the JVM internal threads. All values
     * betweens 0.0 and 1.0 are possible depending of the activities going on
     * in the JVM process and the whole system. If the Java Virtual Machine
     * recent CPU usage is not available, the method returns a negative value.
     *
     * @return the "recent cpu usage" for the Java Virtual Machine process;
     * a negative value if not available.
     */
    public static double processLoad() {
        return BEAN.getProcessCpuLoad();
    }

    public static double processLoad10SecAvg() {
        return PROCESS_AVERAGE_10_SEC.mean();
    }

    public static double processLoad1MinAvg() {
        return PROCESS_AVERAGE_1_MIN.mean();
    }

    public static double processLoad15MinAvg() {
        return PROCESS_AVERAGE_15_MIN.mean();
    }

    /**
     * Task to poll CPU loads and add to the rolling averages in the enclosing class.
     */
    private static final class PollingTask implements Runnable {
        private final RollingAverage[] systemAverages = new RollingAverage[]{
                SYSTEM_AVERAGE_10_SEC,
                SYSTEM_AVERAGE_1_MIN,
                SYSTEM_AVERAGE_15_MIN
        };
        private final RollingAverage[] processAverages = new RollingAverage[]{
                PROCESS_AVERAGE_10_SEC,
                PROCESS_AVERAGE_1_MIN,
                PROCESS_AVERAGE_15_MIN
        };

        @Override
        public void run() {
            double systemLoad = systemLoad();
            double processLoad = processLoad();
            long timeMillis = TimeUtil.monotonicCurrentTimeMillis();

            if (systemLoad >= 0) {
                BigDecimal value = new BigDecimal(systemLoad);
                for (RollingAverage average : this.systemAverages) {
                    average.add(value);
                }

                if (Metrics.shouldRecordCpuUsageSystem(timeMillis)) {
                    Metrics.CPU_USAGE_SYSTEM.record(timeMillis, systemLoad);
                }
            }

            if (processLoad >= 0) {
                BigDecimal value = new BigDecimal(processLoad);
                for (RollingAverage average : this.processAverages) {
                    average.add(value);
                }

                if (Metrics.shouldRecordCpuUsageProcess(timeMillis)) {
                    Metrics.CPU_USAGE_PROCESS.record(timeMillis, processLoad);
                }
            }
        }
    }

    public interface OperatingSystemMXBean {
        double getSystemCpuLoad();
        double getProcessCpuLoad();
    }

}
