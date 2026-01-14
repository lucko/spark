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

package me.lucko.spark.common.platform;

import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.monitor.cpu.CpuInfo;
import me.lucko.spark.common.monitor.cpu.CpuMonitor;
import me.lucko.spark.common.monitor.disk.DiskUsage;
import me.lucko.spark.common.monitor.memory.GarbageCollectorStatistics;
import me.lucko.spark.common.monitor.memory.MemoryInfo;
import me.lucko.spark.common.monitor.net.NetworkInterfaceAverages;
import me.lucko.spark.common.monitor.net.NetworkMonitor;
import me.lucko.spark.common.monitor.os.OperatingSystemInfo;
import me.lucko.spark.common.monitor.ping.PingStatistics;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.platform.world.AsyncWorldInfoProvider;
import me.lucko.spark.common.platform.world.WorldStatisticsProvider;
import me.lucko.spark.proto.SparkProtos;
import me.lucko.spark.proto.SparkProtos.PlatformStatistics;
import me.lucko.spark.proto.SparkProtos.SystemStatistics;
import me.lucko.spark.proto.SparkProtos.WorldStatistics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlatformStatisticsProvider {
    private final SparkPlatform platform;

    public PlatformStatisticsProvider(SparkPlatform platform) {
        this.platform = platform;
    }

    public SystemStatistics getSystemStatistics() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        OperatingSystemInfo osInfo = OperatingSystemInfo.poll();

        String vmArgs = String.join(" ", runtimeBean.getInputArguments());

        SystemStatistics.Builder builder = SystemStatistics.newBuilder()
                .setCpu(SystemStatistics.Cpu.newBuilder()
                        .setThreads(Runtime.getRuntime().availableProcessors())
                        .setProcessUsage(SystemStatistics.Cpu.Usage.newBuilder()
                                .setLast1M(CpuMonitor.processLoad1MinAvg())
                                .setLast15M(CpuMonitor.processLoad15MinAvg())
                                .build()
                        )
                        .setSystemUsage(SystemStatistics.Cpu.Usage.newBuilder()
                                .setLast1M(CpuMonitor.systemLoad1MinAvg())
                                .setLast15M(CpuMonitor.systemLoad15MinAvg())
                                .build()
                        )
                        .setModelName(CpuInfo.queryCpuModel())
                        .build()
                )
                .setMemory(SystemStatistics.Memory.newBuilder()
                        .setPhysical(SystemStatistics.Memory.MemoryPool.newBuilder()
                                .setUsed(MemoryInfo.getUsedPhysicalMemory())
                                .setTotal(MemoryInfo.getTotalPhysicalMemory())
                                .build()
                        )
                        .setSwap(SystemStatistics.Memory.MemoryPool.newBuilder()
                                .setUsed(MemoryInfo.getUsedSwap())
                                .setTotal(MemoryInfo.getTotalSwap())
                                .build()
                        )
                        .build()
                )
                .setDisk(SystemStatistics.Disk.newBuilder()
                        .setTotal(DiskUsage.getTotal())
                        .setUsed(DiskUsage.getUsed())
                        .build()
                )
                .setOs(SystemStatistics.Os.newBuilder()
                        .setArch(osInfo.arch())
                        .setName(osInfo.name())
                        .setVersion(osInfo.version())
                        .build()
                )
                .setJava(SystemStatistics.Java.newBuilder()
                        .setVendor(System.getProperty("java.vendor", "unknown"))
                        .setVersion(System.getProperty("java.version", "unknown"))
                        .setVendorVersion(System.getProperty("java.vendor.version", "unknown"))
                        .setVmArgs(VmArgRedactor.replace(vmArgs))
                        .build()
                )
                .setJvm(SystemStatistics.Jvm.newBuilder()
                        .setName(System.getProperty("java.vm.name", "unknown"))
                        .setVendor(System.getProperty("java.vm.vendor", "unknown"))
                        .setVersion(System.getProperty("java.vm.version", "unknown"))
                        .build()
                );

        long uptime = runtimeBean.getUptime();
        builder.setUptime(uptime);

        Map<String, GarbageCollectorStatistics> gcStats = GarbageCollectorStatistics.pollStats();
        gcStats.forEach((name, statistics) -> builder.putGc(
                name,
                SystemStatistics.Gc.newBuilder()
                        .setTotal(statistics.getCollectionCount())
                        .setAvgTime(statistics.getAverageCollectionTime())
                        .setAvgFrequency(statistics.getAverageCollectionFrequency(uptime))
                        .build()
        ));

        Map<String, NetworkInterfaceAverages> networkInterfaceStats = NetworkMonitor.systemAverages();
        networkInterfaceStats.forEach((name, statistics) -> builder.putNet(
                name,
                SystemStatistics.NetInterface.newBuilder()
                        .setRxBytesPerSecond(rollingAvgProto(statistics.rxBytesPerSecond()))
                        .setRxPacketsPerSecond(rollingAvgProto(statistics.rxPacketsPerSecond()))
                        .setTxBytesPerSecond(rollingAvgProto(statistics.txBytesPerSecond()))
                        .setTxPacketsPerSecond(rollingAvgProto(statistics.txPacketsPerSecond()))
                        .build()
        ));

        return builder.build();
    }

    public PlatformStatistics getPlatformStatistics(Map<String, GarbageCollectorStatistics> startingGcStatistics, boolean includeWorldStatistics) {
        PlatformStatistics.Builder builder = PlatformStatistics.newBuilder();

        PlatformStatistics.Memory.Builder memory = PlatformStatistics.Memory.newBuilder()
                .setHeap(memoryUsageProto(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage()))
                .setNonHeap(memoryUsageProto(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage()));

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPool : memoryPoolMXBeans) {
            if (memoryPool.getType() != MemoryType.HEAP) {
                continue;
            }

            MemoryUsage usage = memoryPool.getUsage();
            MemoryUsage collectionUsage = memoryPool.getCollectionUsage();

            if (usage.getMax() == -1) {
                usage = new MemoryUsage(usage.getInit(), usage.getUsed(), usage.getCommitted(), usage.getCommitted());
            }

            memory.addPools(PlatformStatistics.Memory.MemoryPool.newBuilder()
                    .setName(memoryPool.getName())
                    .setUsage(memoryUsageProto(usage))
                    .setCollectionUsage(memoryUsageProto(collectionUsage))
                    .build()
            );
        }

        builder.setMemory(memory.build());

        long uptime = System.currentTimeMillis() - this.platform.getServerNormalOperationStartTime();
        builder.setUptime(uptime);

        if (startingGcStatistics != null) {
            Map<String, GarbageCollectorStatistics> gcStats = GarbageCollectorStatistics.pollStatsSubtractInitial(startingGcStatistics);
            gcStats.forEach((name, statistics) -> builder.putGc(
                    name,
                    PlatformStatistics.Gc.newBuilder()
                            .setTotal(statistics.getCollectionCount())
                            .setAvgTime(statistics.getAverageCollectionTime())
                            .setAvgFrequency(statistics.getAverageCollectionFrequency(uptime))
                            .build()
            ));
        }

        TickStatistics tickStatistics = this.platform.getTickStatistics();
        if (tickStatistics != null) {
            builder.setTps(PlatformStatistics.Tps.newBuilder()
                    .setLast1M(tickStatistics.tps1Min())
                    .setLast5M(tickStatistics.tps5Min())
                    .setLast15M(tickStatistics.tps15Min())
                    .setGameTargetTps(tickStatistics.gameTargetTps())
                    .build()
            );
            if (tickStatistics.isDurationSupported()) {
                builder.setMspt(PlatformStatistics.Mspt.newBuilder()
                        .setLast1M(rollingAvgProto(tickStatistics.duration1Min()))
                        .setLast5M(rollingAvgProto(tickStatistics.duration5Min()))
                        .setGameMaxIdealMspt(tickStatistics.gameMaxIdealDuration())
                        .build()
                );
            }
        }

        PingStatistics pingStatistics = this.platform.getPingStatistics();
        if (pingStatistics != null && pingStatistics.getPingAverage().getSamples() != 0) {
            builder.setPing(PlatformStatistics.Ping.newBuilder()
                    .setLast15M(rollingAvgProto(pingStatistics.getPingAverage()))
                    .build()
            );
        }

        List<CommandSender> senders = this.platform.getPlugin().getCommandSenders().collect(Collectors.toList());

        PlatformInfo.Type platformType = this.platform.getPlugin().getPlatformInfo().getType();
        if (platformType == PlatformInfo.Type.SERVER || platformType == PlatformInfo.Type.PROXY) {
            long playerCount = senders.size() - 1; // includes console
            builder.setPlayerCount(playerCount);
        }

        UUID anyOnlinePlayerUniqueId = senders.stream()
                .filter(CommandSender::isPlayer)
                .map(CommandSender::getUniqueId)
                .filter(uniqueId -> uniqueId.version() == 4 || uniqueId.version() == 3)
                .findAny()
                .orElse(null);

        builder.setOnlineMode(anyOnlinePlayerUniqueId == null
                ? PlatformStatistics.OnlineMode.UNKNOWN
                : anyOnlinePlayerUniqueId.version() == 4
                    ? PlatformStatistics.OnlineMode.ONLINE
                    : PlatformStatistics.OnlineMode.OFFLINE
        );

        if (includeWorldStatistics) {
            try {
                WorldStatisticsProvider worldStatisticsProvider = new WorldStatisticsProvider(
                        new AsyncWorldInfoProvider(this.platform, this.platform.getPlugin().createWorldInfoProvider())
                );
                WorldStatistics worldStatistics = worldStatisticsProvider.getWorldStatistics();
                if (worldStatistics != null) {
                    builder.setWorld(worldStatistics);
                }
            } catch (Exception e) {
                this.platform.getPlugin().log(Level.WARNING, "Failed to gather world statistics", e);
            }
        }

        return builder.build();
    }

    public static SparkProtos.RollingAverageValues rollingAvgProto(DoubleAverageInfo info) {
        return SparkProtos.RollingAverageValues.newBuilder()
                .setMean(info.mean())
                .setMax(info.max())
                .setMin(info.min())
                .setMedian(info.median())
                .setPercentile95(info.percentile95th())
                .build();
    }

    public static PlatformStatistics.Memory.MemoryUsage memoryUsageProto(MemoryUsage usage) {
        return PlatformStatistics.Memory.MemoryUsage.newBuilder()
                .setUsed(usage.getUsed())
                .setCommitted(usage.getCommitted())
                .setInit(usage.getInit())
                .setMax(usage.getMax())
                .build();
    }

    static final class VmArgRedactor {
        private static final Pattern WINDOWS_USERNAME = Pattern.compile("C:\\\\Users\\\\\\w+");
        private static final Pattern MACOS_USERNAME = Pattern.compile("/Users/\\w+");
        private static final Pattern LINUX_USERNAME = Pattern.compile("/home/\\w+");

        static String replace(String input) {
            input = WINDOWS_USERNAME.matcher(input).replaceAll("C:\\\\Users\\\\<redacted>");
            input = MACOS_USERNAME.matcher(input).replaceAll("/Users/<redacted>");
            input = LINUX_USERNAME.matcher(input).replaceAll("/home/<redacted>");
            return input;
        }
    }

}
