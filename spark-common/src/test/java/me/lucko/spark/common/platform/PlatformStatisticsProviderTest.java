package me.lucko.spark.common.platform;

import me.lucko.spark.proto.SparkProtos;
import me.lucko.spark.test.plugin.TestSparkPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PlatformStatisticsProviderTest {

    @Test
    public void testSystemStatistics(@TempDir Path directory) {
        try (TestSparkPlugin plugin = new TestSparkPlugin(directory)) {
            SparkProtos.SystemStatistics systemStatistics = new PlatformStatisticsProvider(plugin.platform()).getSystemStatistics();
            assertNotNull(systemStatistics);
        }
    }

    @Test
    public void testPlatformStatistics(@TempDir Path directory) {
        try (TestSparkPlugin plugin = new TestSparkPlugin(directory)) {
            SparkProtos.PlatformStatistics platformStatistics = new PlatformStatisticsProvider(plugin.platform()).getPlatformStatistics(null, true);
            assertNotNull(platformStatistics);
        }
    }

}
