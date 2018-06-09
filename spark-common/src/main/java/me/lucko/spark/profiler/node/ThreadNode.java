package me.lucko.spark.profiler.node;

import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * The root of a sampling stack for a given thread / thread group.
 */
public final class ThreadNode extends AbstractNode {

    /**
     * The name of this thread
     */
    private final String threadName;

    public ThreadNode(String threadName) {
        this.threadName = threadName;
    }

    protected void appendMetadata(JsonWriter writer) throws IOException {
        writer.name("name").value(this.threadName);
    }
}
