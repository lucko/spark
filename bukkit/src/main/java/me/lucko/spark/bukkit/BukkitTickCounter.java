package me.lucko.spark.bukkit;

import me.lucko.spark.profiler.TickCounter;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

public class BukkitTickCounter implements TickCounter, Runnable {
    private final Plugin plugin;
    private BukkitTask task;

    private final Set<Runnable> tasks = new HashSet<>();
    private final LongAdder tick = new LongAdder();

    public BukkitTickCounter(Plugin plugin) {
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
        this.task = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, this, 1, 1);
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
