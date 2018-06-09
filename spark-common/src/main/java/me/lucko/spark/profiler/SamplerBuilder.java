package me.lucko.spark.profiler;

import java.util.concurrent.TimeUnit;

/**
 * Builds {@link Sampler} instances.
 */
public class SamplerBuilder {

    private int samplingInterval = 4;
    private long timeout = -1;
    private ThreadDumper threadDumper = ThreadDumper.ALL;
    private ThreadGrouper threadGrouper = ThreadGrouper.BY_NAME;

    private int ticksOver = -1;
    private TickCounter tickCounter = null;

    public SamplerBuilder() {
    }

    public SamplerBuilder samplingInterval(int samplingInterval) {
        this.samplingInterval = samplingInterval;
        return this;
    }

    public SamplerBuilder completeAfter(long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout > 0");
        }
        this.timeout = System.currentTimeMillis() + unit.toMillis(timeout);
        return this;
    }

    public SamplerBuilder threadDumper(ThreadDumper threadDumper) {
        this.threadDumper = threadDumper;
        return this;
    }

    public SamplerBuilder threadGrouper(ThreadGrouper threadGrouper) {
        this.threadGrouper = threadGrouper;
        return this;
    }

    public SamplerBuilder ticksOver(int ticksOver, TickCounter tickCounter) {
        this.ticksOver = ticksOver;
        this.tickCounter = tickCounter;
        return this;
    }

    public Sampler start() {
        Sampler sampler;
        if (this.ticksOver != -1 && this.tickCounter != null) {
            sampler = new Sampler(this.samplingInterval, this.threadDumper, this.threadGrouper, this.timeout, this.tickCounter, this.ticksOver);
        } else {
            sampler = new Sampler(this.samplingInterval, this.threadDumper, this.threadGrouper, this.timeout);
        }

        sampler.start();
        return sampler;
    }

}
