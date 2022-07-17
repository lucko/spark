package me.lucko.spark.api.profiler.report;

import me.lucko.spark.proto.SparkSamplerProtos;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents the result of a profiler.
 */
public interface ProfilerReport {
    /**
     * Uploads this report online.
     * @return the URL of the uploaded report
     */
    String upload() throws IOException;

    /**
     * Gets the data of this report
     * @return the data
     */
    SparkSamplerProtos.SamplerData data();

    /**
     * Saves this report to a local file.
     * @param path the path to save to
     * @return the {@code path}
     * @throws IOException if an exception occurred
     */
    Path saveToFile(Path path) throws IOException;
}
