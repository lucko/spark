package me.lucko.spark.allay;

import me.lucko.spark.common.tick.AbstractTickHook;
import me.lucko.spark.common.tick.TickHook;
import org.allaymc.api.scheduler.Task;
import org.allaymc.api.server.Server;

import java.util.concurrent.atomic.AtomicBoolean;

/*
 * @author IWareQ
 */
public class AllayTickHook extends AbstractTickHook implements TickHook, Task {

    private final AtomicBoolean isEnabled = new AtomicBoolean(true);

    private final AllaySparkPlugin plugin;

    public AllayTickHook(AllaySparkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onRun() {
        this.onTick();
        return this.isEnabled.get();
    }

    @Override
    public void start() {
        Server.getInstance().getScheduler().scheduleRepeating(this.plugin, this, 1);
    }

    @Override
    public void close() {
        this.isEnabled.compareAndSet(true, false);
    }
}
