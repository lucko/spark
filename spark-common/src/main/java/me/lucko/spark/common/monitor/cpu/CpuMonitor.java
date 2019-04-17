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

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public final class CpuMonitor {
    private CpuMonitor() {}

    /** The object name of the com.sun.management.OperatingSystemMXBean */
    private static final String OPERATING_SYSTEM_BEAN = "java.lang:type=OperatingSystem";

    private static OperatingSystemMXBean getBean() {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName diagnosticBeanName = ObjectName.getInstance(OPERATING_SYSTEM_BEAN);
            return JMX.newMXBeanProxy(beanServer, diagnosticBeanName, OperatingSystemMXBean.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
    public static double getSystemCpuLoad() {
        return getBean().getSystemCpuLoad();
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
    public static double getProcessCpuLoad() {
        return getBean().getProcessCpuLoad();
    }

    public interface OperatingSystemMXBean {
        double getSystemCpuLoad();
        double getProcessCpuLoad();
    }

}
