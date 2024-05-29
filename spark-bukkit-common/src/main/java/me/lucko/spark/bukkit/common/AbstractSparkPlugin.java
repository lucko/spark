package me.lucko.spark.bukkit.common;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import me.lucko.spark.api.Spark;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

public interface AbstractSparkPlugin extends SparkPlugin, Plugin {

    @Override
    default String getVersion() {
        return this.getDescription().getVersion();
    }

    @Override
    default Path getPluginDirectory() {
        return this.getDataFolder().toPath();
    }

    @Override
    default String getCommandName() {
        return "spark";
    }

    @Override
    default void log(Level level, String msg) {
        this.getLogger().log(level, msg);
    }

    @Override
    default Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
            Arrays.asList(this.getServer().getPluginManager().getPlugins()),
            Plugin::getName,
            plugin -> plugin.getDescription().getVersion(),
            plugin -> String.join(", ", plugin.getDescription().getAuthors())
        );
    }

    @Override
    default void executeAsync(Runnable task) {
        this.getServer().getScheduler().runTaskAsynchronously(this, task);
    }

    @Override
    default void executeSync(Runnable task) {
        this.getServer().getScheduler().runTask(this, task);
    }

    @Override
    default void registerApi(Spark api) {
        this.getServer().getServicesManager().register(Spark.class, api, this, ServicePriority.Normal);
    }
}
