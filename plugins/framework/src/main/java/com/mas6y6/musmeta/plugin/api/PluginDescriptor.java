package com.mas6y6.musmeta.plugin.api;

import java.util.List;

public record PluginDescriptor(
        String id,
        String name,
        String version,
        String description,
        List<String> authors,
        String main,
        List<String> mixins,
        List<String> dependencies) {

    public PluginDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Plugin id is required");
        }
        authors = authors == null ? List.of() : List.copyOf(authors);
        mixins = mixins == null ? List.of() : List.copyOf(mixins);
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }
}