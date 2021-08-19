package me.lucko.spark.api.profiler;

public interface Profiler {

    boolean start(ProfilerConfiguration configuration);

    ProfilerReport stop();
}
