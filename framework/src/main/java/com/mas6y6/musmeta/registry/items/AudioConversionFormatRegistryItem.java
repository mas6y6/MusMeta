package com.mas6y6.musmeta.registry.items;

import java.util.List;

public record AudioConversionFormatRegistryItem(String extension, String codec, List<String> codecArguments, String displayName) {
    public AudioConversionFormatRegistryItem {
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("Audio conversion format extension is required");
        }
        if (codec == null || codec.isBlank()) {
            throw new IllegalArgumentException("Audio conversion format codec is required");
        }
        codecArguments = codecArguments == null ? List.of() : List.copyOf(codecArguments);
        displayName = displayName == null || displayName.isBlank() ? extension.toUpperCase() : displayName;
    }
}