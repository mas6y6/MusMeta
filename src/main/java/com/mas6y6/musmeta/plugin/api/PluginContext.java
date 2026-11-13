package com.mas6y6.musmeta.plugin.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mas6y6.musmeta.event.core.EventBus;
import com.mas6y6.musmeta.registry.Registries;

import java.nio.file.Path;

public final class PluginContext {

    private final PluginDescriptor descriptor;
    private final Path dataDirectory;

    public PluginContext(PluginDescriptor descriptor, Path dataDirectory) {
        this.descriptor = descriptor;
        this.dataDirectory = dataDirectory;
    }

    public PluginDescriptor descriptor() {
        return descriptor;
    }

    public String id() {
        return descriptor.id();
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public Logger logger() {
        return LoggerFactory.getLogger("plugin." + descriptor.id());
    }

    public EventBus eventBus() {
        return EventBus.MAIN_BUS;
    }
}