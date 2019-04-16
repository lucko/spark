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

package me.lucko.spark.common.memory;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * Utility for creating .hprof memory heap snapshots.
 */
public final class HeapDump {

    private HeapDump() {}

    /** The object name of the com.sun.management.HotSpotDiagnosticMXBean */
    private static final String DIAGNOSTIC_BEAN = "com.sun.management:type=HotSpotDiagnostic";

    /**
     * Creates a heap dump at the given output path.
     *
     * @param outputPath the path to write the snapshot to
     * @param live if true dump only live objects i.e. objects that are reachable from others
     * @throws Exception catch all
     */
    public static void dumpHeap(Path outputPath, boolean live) throws Exception {
        String outputPathString = outputPath.toAbsolutePath().normalize().toString();

        if (isOpenJ9()) {
            Class<?> dumpClass = Class.forName("com.ibm.jvm.Dump");
            Method heapDumpMethod = dumpClass.getMethod("heapDumpToFile", String.class);
            heapDumpMethod.invoke(null, outputPathString);
        } else {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName diagnosticBeanName = ObjectName.getInstance(DIAGNOSTIC_BEAN);

            HotSpotDiagnosticMXBean proxy = JMX.newMXBeanProxy(beanServer, diagnosticBeanName, HotSpotDiagnosticMXBean.class);
            proxy.dumpHeap(outputPathString, live);
        }
    }

    public static boolean isOpenJ9() {
        try {
            Class.forName("com.ibm.jvm.Dump");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public interface HotSpotDiagnosticMXBean {
        void dumpHeap(String outputFile, boolean live) throws IOException;
    }

}
