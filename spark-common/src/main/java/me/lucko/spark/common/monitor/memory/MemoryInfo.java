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

import me.lucko.spark.common.util.LinuxProc;

import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Utility to query information about system memory usage.
 */
public enum MemoryInfo {
    ;

    /** The object name of the com.sun.management.OperatingSystemMXBean */
    private static final String OPERATING_SYSTEM_BEAN = "java.lang:type=OperatingSystem";
    /** The OperatingSystemMXBean instance */
    private static final OperatingSystemMXBean BEAN;

    /** The format used by entries in /proc/meminfo */
    private static final Pattern PROC_MEMINFO_VALUE = Pattern.compile("^(\\w+):\\s*(\\d+) kB$");

    static {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName diagnosticBeanName = ObjectName.getInstance(OPERATING_SYSTEM_BEAN);
            BEAN = JMX.newMXBeanProxy(beanServer, diagnosticBeanName, OperatingSystemMXBean.class);
        } catch (Exception e) {
            throw new UnsupportedOperationException("OperatingSystemMXBean is not supported by the system", e);
        }
    }

    /* Swap */

    public static long getUsedSwap() {
        return BEAN.getTotalSwapSpaceSize() - BEAN.getFreeSwapSpaceSize();
    }

    public static long getTotalSwap() {
        return BEAN.getTotalSwapSpaceSize();
    }

    /* Physical Memory */

    public static long getUsedPhysicalMemory() {
        return getTotalPhysicalMemory() - getAvailablePhysicalMemory();
    }

    public static long getTotalPhysicalMemory() {
        // try to read from /proc/meminfo on linux systems
        for (String line : LinuxProc.MEMINFO.read()) {
            Matcher matcher = PROC_MEMINFO_VALUE.matcher(line);
            if (matcher.matches()) {
                String label = matcher.group(1);
                long value = Long.parseLong(matcher.group(2)) * 1024; // kB -> B

                if (label.equals("MemTotal")) {
                    return value;
                }
            }
        }

        // fallback to JVM measure
        return BEAN.getTotalPhysicalMemorySize();
    }

    public static long getAvailablePhysicalMemory() {
        boolean present = false;
        long free = 0, buffers = 0, cached = 0, sReclaimable = 0;

        for (String line : LinuxProc.MEMINFO.read()) {
            Matcher matcher = PROC_MEMINFO_VALUE.matcher(line);
            if (matcher.matches()) {
                present = true;

                String label = matcher.group(1);
                long value = Long.parseLong(matcher.group(2)) * 1024; // kB -> B

                // if MemAvailable is set, just return that
                if (label.equals("MemAvailable")) {
                    return value;
                }

                // otherwise, record MemFree, Buffers, Cached and SReclaimable
                switch (label) {
                    case "MemFree":
                        free = value;
                        break;
                    case "Buffers":
                        buffers = value;
                        break;
                    case "Cached":
                        cached = value;
                        break;
                    case "SReclaimable":
                        sReclaimable = value;
                        break;
                }
            }
        }

        // estimate how much is "available" - not exact but this is probably good enough.
        // most Linux systems (assuming they have been updated in the last ~8 years) will
        // have MemAvailable set, and we return that instead if present
        //
        // useful ref: https://www.linuxatemyram.com/
        if (present) {
            return free + buffers + cached + sReclaimable;
        }

        // fallback to what the JVM understands as "free"
        // on non-linux systems, this is probably good enough to estimate what's available
        return BEAN.getFreePhysicalMemorySize();
    }

    /* Virtual Memory */

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
