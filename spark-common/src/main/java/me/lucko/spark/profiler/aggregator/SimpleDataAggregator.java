package me.lucko.spark.profiler.aggregator;

import me.lucko.spark.profiler.ThreadGrouper;
import me.lucko.spark.profiler.node.AbstractNode;
import me.lucko.spark.profiler.node.ThreadNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Basic implementation of {@link DataAggregator}.
 */
public class SimpleDataAggregator implements DataAggregator {

    /** A map of root stack nodes for each thread with sampling data */
    private final Map<String, ThreadNode> threadData = new ConcurrentHashMap<>();

    /** The worker pool used for sampling */
    private final ExecutorService workerPool;

    /** The instance used to group threads together */
    private final ThreadGrouper threadGrouper;

    /** The interval to wait between sampling, in milliseconds */
    private final int interval;

    public SimpleDataAggregator(ExecutorService workerPool, ThreadGrouper threadGrouper, int interval) {
        this.workerPool = workerPool;
        this.threadGrouper = threadGrouper;
        this.interval = interval;
    }

    @Override
    public void insertData(String threadName, StackTraceElement[] stack) {
        try {
            String group = this.threadGrouper.getGroup(threadName);
            AbstractNode node = this.threadData.computeIfAbsent(group, ThreadNode::new);
            node.log(stack, this.interval);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, ThreadNode> getData() {
        // wait for all pending data to be inserted
        this.workerPool.shutdown();
        try {
            this.workerPool.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return this.threadData;
    }
}
