package com.mas6y6.musmeta.registry.items;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public record MainTabRegistryItem(String title, @Nullable Icon icon, ComponentBuilder tabBuilder, boolean closable) {
    public MainTabRegistryItem {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Main tab title is required");
        }
        if (tabBuilder == null) {
            throw new IllegalArgumentException("Main tab builder is required");
        }
    }

    public MainTabRegistryItem(String title, @Nullable Icon icon, ComponentBuilder tabBuilder) {
        this(title, icon, tabBuilder, true);
    }
}