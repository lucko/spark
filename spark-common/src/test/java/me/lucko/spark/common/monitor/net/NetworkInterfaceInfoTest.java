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

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NetworkInterfaceInfoTest {

    @Test
    public void testLinuxProcParse() {
        String input =
                "Inter-|   Receive                                                |  Transmit\n" +
                " face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed\n" +
                "    lo: 2776770   11307    0    0    0     0          0         0  2776770   11307    0    0    0     0       0          0\n" +
                "  eth0: 1215645    2751    1    0    0     0          0         0  1782404    4324    2    0    0   427       0          0\n" +
                "  ppp0: 1622270    5552    1    0    0     0          0         0   354130    5669    0    0    0     0       0          0\n" +
                "  tap0:    7714      81    0    0    0     0          0         0     7714      81    0    0    0     0       0          0";

        Map<String, NetworkInterfaceInfo> map = NetworkInterfaceInfo.read(Arrays.asList(input.split("\n")));
        assertNotNull(map);
        assertEquals(ImmutableSet.of("lo", "eth0", "ppp0", "tap0"), map.keySet());

        NetworkInterfaceInfo eth0 = map.get("eth0");
        assertEquals(1215645, eth0.getReceivedBytes());
        assertEquals(2751, eth0.getReceivedPackets());
        assertEquals(1, eth0.getReceiveErrors());
        assertEquals(1782404, eth0.getTransmittedBytes());
        assertEquals(4324, eth0.getTransmittedPackets());
        assertEquals(2, eth0.getTransmitErrors());
    }

}
