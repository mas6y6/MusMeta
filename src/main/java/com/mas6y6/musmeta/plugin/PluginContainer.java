package com.mas6y6.musmeta.plugin;

import com.mas6y6.musmeta.plugin.api.Plugin;
import com.mas6y6.musmeta.plugin.api.PluginDescriptor;
import com.mas6y6.musmeta.plugin.api.PluginState;

import java.nio.file.Path;

public final class PluginContainer {

    private final PluginDescriptor descriptor;
    private final Path source;
    private final PluginClassLoader classLoader;
    private Plugin plugin;
    private PluginState state = PluginState.LOADED;

    public PluginContainer(PluginDescriptor descriptor, Path source, PluginClassLoader classLoader) {
        this.descriptor = descriptor;
        this.source = source;
        this.classLoader = classLoader;
    }

    public PluginDescriptor descriptor() {
        return descriptor;
    }

    public String id() {
        return descriptor.id();
    }

    public Path source() {
        return source;
    }

    public PluginClassLoader classLoader() {
        return classLoader;
    }

    public Plugin plugin() {
        return plugin;
    }

    public PluginState state() {
        return state;
    }

    void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    void setState(PluginState state) {
        this.state = state;
    }
}