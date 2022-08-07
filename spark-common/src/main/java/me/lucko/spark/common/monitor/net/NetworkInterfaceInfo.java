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

package me.lucko.spark.common.monitor.net;

import com.google.common.collect.ImmutableMap;

import me.lucko.spark.common.monitor.LinuxProc;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Exposes information/statistics about a network interface.
 */
public final class NetworkInterfaceInfo {
    public static final NetworkInterfaceInfo ZERO = new NetworkInterfaceInfo("", 0, 0, 0, 0, 0, 0);

    private final String name;
    private final long rxBytes;
    private final long rxPackets;
    private final long rxErrors;
    private final long txBytes;
    private final long txPackets;
    private final long txErrors;

    public NetworkInterfaceInfo(String name, long rxBytes, long rxPackets, long rxErrors, long txBytes, long txPackets, long txErrors) {
        this.name = name;
        this.rxBytes = rxBytes;
        this.rxPackets = rxPackets;
        this.rxErrors = rxErrors;
        this.txBytes = txBytes;
        this.txPackets = txPackets;
        this.txErrors = txErrors;
    }

    /**
     * Gets the name of the network interface.
     *
     * @return the interface name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the total number of bytes of data received by the interface.
     *
     * @return the total received bytes
     */
    public long getReceivedBytes() {
        return this.rxBytes;
    }

    /**
     * Gets the total number of packets of data received by the interface.
     *
     * @return the total received packets
     */
    public long getReceivedPackets() {
        return this.rxPackets;
    }

    /**
     * Gets the total number of receive errors detected by the device driver.
     *
     * @return the total receive errors
     */
    public long getReceiveErrors() {
        return this.rxErrors;
    }

    /**
     * Gets the total number of bytes of data transmitted by the interface.
     *
     * @return the total transmitted bytes
     */
    public long getTransmittedBytes() {
        return this.txBytes;
    }

    /**
     * Gets the total number of packets of data transmitted by the interface.
     *
     * @return the total transmitted packets
     */
    public long getTransmittedPackets() {
        return this.txPackets;
    }

    /**
     * Gets the total number of transmit errors detected by the device driver.
     *
     * @return the total transmit errors
     */
    public long getTransmitErrors() {
        return this.txErrors;
    }

    public long getBytes(Direction direction) {
        switch (direction) {
            case RECEIVE:
                return getReceivedBytes();
            case TRANSMIT:
                return getTransmittedBytes();
            default:
                throw new AssertionError();
        }
    }

    public long getPackets(Direction direction) {
        switch (direction) {
            case RECEIVE:
                return getReceivedPackets();
            case TRANSMIT:
                return getTransmittedPackets();
            default:
                throw new AssertionError();
        }
    }

    public boolean isZero() {
        return this.rxBytes == 0 && this.rxPackets == 0 && this.rxErrors == 0 &&
                this.txBytes == 0 && this.txPackets == 0 && this.txErrors == 0;
    }

    public NetworkInterfaceInfo subtract(NetworkInterfaceInfo other) {
        if (other == ZERO || other.isZero()) {
            return this;
        }

        return new NetworkInterfaceInfo(
                this.name,
                this.rxBytes - other.rxBytes,
                this.rxPackets - other.rxPackets,
                this.rxErrors - other.rxErrors,
                this.txBytes - other.txBytes,
                this.txPackets - other.txPackets,
                this.txErrors - other.txErrors
        );
    }

    /**
     * Calculate the difference between two readings in order to calculate the rate.
     *
     * @param current the polled values
     * @param previous the previously polled values
     * @return the difference
     */
    public static @NonNull Map<String, NetworkInterfaceInfo> difference(Map<String, NetworkInterfaceInfo> current, Map<String, NetworkInterfaceInfo> previous) {
        if (previous == null || previous.isEmpty()) {
            return current;
        }

        ImmutableMap.Builder<String, NetworkInterfaceInfo> builder = ImmutableMap.builder();
        for (NetworkInterfaceInfo netInf : current.values()) {
            String name = netInf.getName();
            builder.put(name, netInf.subtract(previous.getOrDefault(name, ZERO)));
        }
        return builder.build();
    }

    /**
     * Queries the network interface statistics for the system.
     *
     * <p>Returns an empty {@link Map} if no statistics could be gathered.</p>
     *
     * @return the system net stats
     */
    public static @NonNull Map<String, NetworkInterfaceInfo> pollSystem() {
        try {
            List<String> output = LinuxProc.NET_DEV.read();
            return read(output);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static final Pattern PROC_NET_DEV_PATTERN = Pattern.compile("^\\s*(\\w+):([\\d\\s]+)$");

    private static @NonNull Map<String, NetworkInterfaceInfo> read(List<String> output) {
        // Inter-|   Receive                                                |  Transmit
        //  face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
        //     lo: 2776770   11307    0    0    0     0          0         0  2776770   11307    0    0    0     0       0          0
        //   eth0: 1215645    2751    0    0    0     0          0         0  1782404    4324    0    0    0   427       0          0
        //   ppp0: 1622270    5552    1    0    0     0          0         0   354130    5669    0    0    0     0       0          0
        //   tap0:    7714      81    0    0    0     0          0         0     7714      81    0    0    0     0       0          0

        if (output.size() < 3) {
            // no data
            return Collections.emptyMap();
        }

        String header = output.get(1);
        String[] categories = header.split("\\|");
        if (categories.length != 3) {
            // unknown format
            return Collections.emptyMap();
        }

        List<String> rxFields = Arrays.asList(categories[1].trim().split("\\s+"));
        List<String> txFields = Arrays.asList(categories[2].trim().split("\\s+"));

        int rxFieldsLength = rxFields.size();
        int txFieldsLength = txFields.size();

        int fieldRxBytes = rxFields.indexOf("bytes");
        int fieldRxPackets = rxFields.indexOf("packets");
        int fieldRxErrors = rxFields.indexOf("errs");

        int fieldTxBytes = rxFieldsLength + txFields.indexOf("bytes");
        int fieldTxPackets = rxFieldsLength + txFields.indexOf("packets");
        int fieldTxErrors = rxFieldsLength + txFields.indexOf("errs");

        int expectedFields = rxFieldsLength + txFieldsLength;

        if (IntStream.of(fieldRxBytes, fieldRxPackets, fieldRxErrors, fieldTxBytes, fieldTxPackets, fieldTxErrors).anyMatch(i -> i == -1)) {
            // missing required fields
            return Collections.emptyMap();
        }

        ImmutableMap.Builder<String, NetworkInterfaceInfo> builder = ImmutableMap.builder();

        for (String line : output.subList(2, output.size())) {
            Matcher matcher = PROC_NET_DEV_PATTERN.matcher(line);
            if (matcher.matches()) {
                String interfaceName = matcher.group(1);
                String[] stringValues = matcher.group(2).trim().split("\\s+");

                if (stringValues.length != expectedFields) {
                    continue;
                }

                long[] values = Arrays.stream(stringValues).mapToLong(Long::parseLong).toArray();
                builder.put(interfaceName, new NetworkInterfaceInfo(
                        interfaceName,
                        values[fieldRxBytes],
                        values[fieldRxPackets],
                        values[fieldRxErrors],
                        values[fieldTxBytes],
                        values[fieldTxPackets],
                        values[fieldTxErrors]
                ));
            }
        }

        return builder.build();
    }

}
