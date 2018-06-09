package me.lucko.spark.forge;

import me.lucko.spark.profiler.TickCounter;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

public class ForgeTickCounter implements TickCounter {
    private final TickEvent.Type type;

    private final Set<Runnable> tasks = new HashSet<>();
    private final LongAdder tick = new LongAdder();

    public ForgeTickCounter(TickEvent.Type type) {
        this.type = type;
    }

    @SubscribeEvent
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }

        if (e.type != this.type) {
            return;
        }

        this.tick.increment();
        for (Runnable r : this.tasks){
            r.run();
        }
    }

    @Override
    public void start() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void close() {
        MinecraftForge.EVENT_BUS.unregister(this);
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
