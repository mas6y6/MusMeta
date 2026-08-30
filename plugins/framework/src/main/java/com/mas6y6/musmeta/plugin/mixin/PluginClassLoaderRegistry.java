package com.mas6y6.musmeta.plugin.mixin;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PluginClassLoaderRegistry {

    private static final List<ClassLoader> LOADERS = new CopyOnWriteArrayList<>();

    private PluginClassLoaderRegistry() {
    }

    public static void register(ClassLoader loader) {
        if (loader != null && !LOADERS.contains(loader)) {
            LOADERS.add(loader);
        }
    }

    public static void unregister(ClassLoader loader) {
        LOADERS.remove(loader);
    }

    public static void clear() {
        LOADERS.clear();
    }

    static List<ClassLoader> pluginLoaders() {
        return List.copyOf(LOADERS);
    }
}