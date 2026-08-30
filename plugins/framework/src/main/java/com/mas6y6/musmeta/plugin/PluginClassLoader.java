package com.mas6y6.musmeta.plugin;

import com.mas6y6.musmeta.plugin.mixin.PluginClassLoaderRegistry;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public final class PluginClassLoader extends URLClassLoader {

    private final String id;

    public PluginClassLoader(String id, Path jar, ClassLoader parent) throws IOException {
        super(new URL[]{jar.toUri().toURL()}, parent);
        this.id = id;
        PluginClassLoaderRegistry.register(this);
    }

    public String getId() {
        return id;
    }

    public void link(Path dependencyJar) throws IOException {
        addURL(dependencyJar.toUri().toURL());
    }

    @Override
    public void close() throws IOException {
        try {
            PluginClassLoaderRegistry.unregister(this);
        } finally {
            super.close();
        }
    }
}