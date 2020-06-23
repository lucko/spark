package me.lucko.spark.common.platform;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

public abstract class AbstractPlatformInfo implements PlatformInfo {

    @Override
    public int getNCpus() {
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    public MemoryUsage getHeapUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }
}
