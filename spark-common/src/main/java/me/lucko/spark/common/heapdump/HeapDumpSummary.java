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

package me.lucko.spark.common.heapdump;

import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.PlatformInfo;
import me.lucko.spark.proto.SparkProtos;
import me.lucko.spark.proto.SparkProtos.HeapData;
import me.lucko.spark.proto.SparkProtos.HeapEntry;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Represents a "heap dump summary" from the VM.
 *
 * <p>Contains a number of entries, corresponding to types of objects in the virtual machine
 * and their recorded impact on memory usage.</p>
 */
public final class HeapDumpSummary {

    /** The object name of the com.sun.management.DiagnosticCommandMBean */
    private static final String DIAGNOSTIC_BEAN = "com.sun.management:type=DiagnosticCommand";
    /** A regex pattern representing the expected format of the raw heap output */
    private static final Pattern OUTPUT_FORMAT = Pattern.compile("^\\s*(\\d+):\\s*(\\d+)\\s*(\\d+)\\s*([^\\s]+).*$");

    /**
     * Obtains the raw heap data output from the DiagnosticCommandMBean.
     *
     * @return the raw output
     * @throws Exception lots could go wrong!
     */
    private static String getRawHeapData() throws Exception {
        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName diagnosticBeanName = ObjectName.getInstance(DIAGNOSTIC_BEAN);

        DiagnosticCommandMXBean proxy = JMX.newMXBeanProxy(beanServer, diagnosticBeanName, DiagnosticCommandMXBean.class);
        return proxy.gcClassHistogram(new String[0]);
    }

    /**
     * Converts type descriptors to their class name.
     *
     * @param type the type
     * @return the class name
     */
    private static String typeToClassName(String type) {
        try {
            return Type.getType(type).getClassName();
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    /**
     * Creates a new heap dump based on the current VM.
     *
     * @return the created heap dump
     * @throws RuntimeException if an error occurred whilst requesting a heap dump from the VM
     */
    public static HeapDumpSummary createNew() {
        String rawOutput;
        try {
            rawOutput = getRawHeapData();
        } catch (Exception e) {
            throw new RuntimeException("Unable to get heap dump", e);
        }

        return new HeapDumpSummary(Arrays.stream(rawOutput.split("\n"))
                .map(line -> {
                    Matcher matcher = OUTPUT_FORMAT.matcher(line);
                    if (!matcher.matches()) {
                        return null;
                    }

                    try {
                        return new Entry(
                                Integer.parseInt(matcher.group(1)),
                                Integer.parseInt(matcher.group(2)),
                                Long.parseLong(matcher.group(3)),
                                typeToClassName(matcher.group(4))
                        );
                    } catch (Exception e) {
                        new IllegalArgumentException("Exception parsing line: " + line, e).printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    /** The entries in this heap dump */
    private final List<Entry> entries;

    private HeapDumpSummary(List<Entry> entries) {
        this.entries = entries;
    }

    private HeapData toProto(PlatformInfo platformInfo, CommandSender creator) {
        HeapData.Builder proto = HeapData.newBuilder();
        proto.setMetadata(SparkProtos.HeapMetadata.newBuilder()
                .setPlatform(platformInfo.toData().toProto())
                .setUser(creator.toData().toProto())
                .build()
        );

        for (Entry entry : this.entries) {
            proto.addEntries(entry.toProto());
        }

        return proto.build();
    }

    public byte[] formCompressedDataPayload(PlatformInfo platformInfo, CommandSender creator) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (OutputStream out = new GZIPOutputStream(byteOut)) {
            toProto(platformInfo, creator).writeTo(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteOut.toByteArray();
    }

    public static final class Entry {
        private final int order;
        private final int instances;
        private final long bytes;
        private final String type;

        Entry(int order, int instances, long bytes, String type) {
            this.order = order;
            this.instances = instances;
            this.bytes = bytes;
            this.type = type;
        }

        public int getOrder() {
            return this.order;
        }

        public int getInstances() {
            return this.instances;
        }

        public long getBytes() {
            return this.bytes;
        }

        public String getType() {
            return this.type;
        }

        public HeapEntry toProto() {
            return HeapEntry.newBuilder()
                    .setOrder(this.order)
                    .setInstances(this.instances)
                    .setSize(this.bytes)
                    .setType(this.type)
                    .build();
        }
    }

    public interface DiagnosticCommandMXBean {
        String gcClassHistogram(String[] args);
    }

}
