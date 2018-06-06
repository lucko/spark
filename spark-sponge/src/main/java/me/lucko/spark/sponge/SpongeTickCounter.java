package me.lucko.spark.sponge;

import me.lucko.spark.profiler.TickCounter;

import org.spongepowered.api.scheduler.Task;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

public class SpongeTickCounter implements TickCounter, Runnable {
    private final SparkSpongePlugin plugin;
    private Task task;

    private final Set<Runnable> tasks = new HashSet<>();
    private final LongAdder tick = new LongAdder();

    public SpongeTickCounter(SparkSpongePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        this.tick.increment();
        for (Runnable r : this.tasks){
            r.run();
        }
    }

    @Override
    public void start() {
        this.task = Task.builder().intervalTicks(1).name("spark-ticker").execute(this).submit(this.plugin);
    }

    @Override
    public void close() {
        this.task.cancel();
    }

    @Override
    public long getCurrentTick() {
        return this.tick.longValue();
    }

    @Override
    public void addTickTask(Runnable runnable) {
        this.tasks.add(runnable);
    }

    @Override
    public void removeTickTask(Runnable runnable) {
        this.tasks.remove(runnable);
    }
}
