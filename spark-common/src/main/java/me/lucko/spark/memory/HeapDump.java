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

package me.lucko.spark.memory;

import com.google.gson.stream.JsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
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
 * Represents a "heap dump" from the VM.
 *
 * <p>Contains a number of entries, corresponding to types of objects in the virtual machine
 * and their recorded impact on memory usage.</p>
 */
public class HeapDump {

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
     * Creates a new heap dump based on the current VM.
     *
     * @return the created heap dump
     * @throws RuntimeException if an error occurred whilst requesting a heap dump from the VM
     */
    public static HeapDump createNew() {
        String rawOutput;
        try {
            rawOutput = getRawHeapData();
        } catch (Exception e) {
            throw new RuntimeException("Unable to get heap dump", e);
        }

        return new HeapDump(Arrays.stream(rawOutput.split("\n"))
                .map(line -> {
                    Matcher matcher = OUTPUT_FORMAT.matcher(line);
                    if (!matcher.matches()) {
                        return null;
                    }

                    return new Entry(
                            Integer.parseInt(matcher.group(1)),
                            Integer.parseInt(matcher.group(2)),
                            Long.parseLong(matcher.group(3)),
                            getFriendlyTypeName(matcher.group(4))
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    /** The entries in this heap dump */
    private final List<Entry> entries;

    private HeapDump(List<Entry> entries) {
        this.entries = entries;
    }

    private void writeOutput(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("type").value("heap");
        writer.name("entries").beginArray();
        for (Entry entry : this.entries) {
            writer.beginObject();
            writer.name("#").value(entry.getOrder());
            writer.name("i").value(entry.getInstances());
            writer.name("s").value(entry.getBytes());
            writer.name("t").value(entry.getType());
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }

    public byte[] formCompressedDataPayload() {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(byteOut), StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = new JsonWriter(writer)) {
                writeOutput(jsonWriter);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteOut.toByteArray();
    }

    private static String getPrimitiveTypeName(char character) {
        switch (character) {
            case 'B':
                return "byte";
            case 'C':
                return "char";
            case 'D':
                return "double";
            case 'F':
                return "float";
            case 'I':
                return "int";
            case 'J':
                return "long";
            case 'S':
                return "short";
            case 'V':
                return "void";
            case 'Z':
                return "boolean";
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String getFriendlyTypeName(String internalDesc) {
        if (internalDesc.length() == 2 && internalDesc.charAt(0) == '[') {
            return getPrimitiveTypeName(internalDesc.charAt(1)) + " array";
        }
        if (internalDesc.startsWith("[L") && internalDesc.endsWith(";")) {
            return internalDesc.substring(2, internalDesc.length() - 1) + " array";
        }
        return internalDesc;
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
    }

    public interface DiagnosticCommandMXBean {
        String gcClassHistogram(String[] args);
    }

}
