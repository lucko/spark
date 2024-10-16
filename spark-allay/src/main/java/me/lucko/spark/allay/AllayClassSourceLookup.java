package me.lucko.spark.allay;

import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import org.allaymc.api.plugin.PluginManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author IWareQ
 */
public class AllayClassSourceLookup extends ClassSourceLookup.ByFirstUrlSource {

    private final Map<ClassLoader, String> classLoaders2PluginName = new HashMap<>();

    public AllayClassSourceLookup(PluginManager manager) {
        manager.getEnabledPlugins().values().forEach(container -> classLoaders2PluginName.put(
                container.plugin().getClass().getClassLoader(),
                container.descriptor().getName())
        );
    }

    @Override
    public String identify(ClassLoader loader) {
        if (!this.classLoaders2PluginName.containsKey(loader)) return null;
        return this.classLoaders2PluginName.get(loader);
    }
}
