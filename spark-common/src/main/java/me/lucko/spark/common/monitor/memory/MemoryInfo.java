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

package me.lucko.spark.common.monitor.memory;

import java.lang.management.ManagementFactory;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public enum MemoryInfo {
    ;

    /** The object name of the com.sun.management.OperatingSystemMXBean */
    private static final String OPERATING_SYSTEM_BEAN = "java.lang:type=OperatingSystem";
    /** The OperatingSystemMXBean instance */
    private static final OperatingSystemMXBean BEAN;

    static {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName diagnosticBeanName = ObjectName.getInstance(OPERATING_SYSTEM_BEAN);
            BEAN = JMX.newMXBeanProxy(beanServer, diagnosticBeanName, OperatingSystemMXBean.class);
        } catch (Exception e) {
            throw new UnsupportedOperationException("OperatingSystemMXBean is not supported by the system", e);
        }
    }

    public static long getUsedSwap() {
        return BEAN.getTotalSwapSpaceSize() - BEAN.getFreeSwapSpaceSize();
    }

    public static long getTotalSwap() {
        return BEAN.getTotalSwapSpaceSize();
    }

    public static long getUsedPhysicalMemory() {
        return BEAN.getTotalPhysicalMemorySize() - BEAN.getFreePhysicalMemorySize();
    }

    public static long getTotalPhysicalMemory() {
        return BEAN.getTotalPhysicalMemorySize();
    }

    public static long getTotalVirtualMemory() {
        return BEAN.getCommittedVirtualMemorySize();
    }

    public interface OperatingSystemMXBean {
        long getCommittedVirtualMemorySize();
        long getTotalSwapSpaceSize();
        long getFreeSwapSpaceSize();
        long getFreePhysicalMemorySize();
        long getTotalPhysicalMemorySize();
    }
}
