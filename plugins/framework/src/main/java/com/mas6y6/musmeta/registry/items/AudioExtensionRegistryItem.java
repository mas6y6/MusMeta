package com.mas6y6.musmeta.registry.items;

public record AudioExtensionRegistryItem(String extension, String description) {
    public AudioExtensionRegistryItem {
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("Audio extension is required");
        }
        description = description == null ? "" : description;
    }
}