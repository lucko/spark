package me.lucko.spark.minestom;

import me.lucko.spark.common.util.ClassSourceLookup;
import net.minestom.server.MinecraftServer;
import net.minestom.server.extensions.Extension;
import net.minestom.server.extensions.ExtensionClassLoader;

import java.util.HashMap;
import java.util.Map;

public class MinestomClassSourceLookup extends ClassSourceLookup.ByClassLoader {
    private final Map<ClassLoader, String> classLoaderToExtensions;

    public MinestomClassSourceLookup() {
        this.classLoaderToExtensions = new HashMap<>();
        for (Extension extension : MinecraftServer.getExtensionManager().getExtensions()) {
            this.classLoaderToExtensions.put(extension.getClass().getClassLoader(), extension.getOrigin().getName());
        }
    }

    @Override
    public String identify(ClassLoader loader) {
        if (loader instanceof ExtensionClassLoader) {
            return this.classLoaderToExtensions.get(loader);
        }
        return null;
    }
}
