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

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PingStatisticsTest {

    @Test
    public void testEmpty() {
        PingStatistics stats = new PingStatistics(ImmutableMap::of, null);

        PingSummary summary = stats.currentSummary();
        assertEquals(0, summary.total());
        assertNull(stats.query("player"));
    }

    @Test
    public void testSimple() {
        PingStatistics stats = new PingStatistics(() -> ImmutableMap.of("Player1", 100, "Player2", 200, "Player3", 300), null);

        PingSummary summary = stats.currentSummary();
        assertEquals(600, summary.total());

        for (PingStatistics.PlayerPing playerPing : new PingStatistics.PlayerPing[]{
                stats.query("Player1"),
                stats.query("player1") // case insensitive
        }) {
            assertNotNull(playerPing);
            assertEquals(100, playerPing.ping());
            assertEquals("Player1", playerPing.name());
        }
    }

}
