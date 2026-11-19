package com.mas6y6.musmeta.registry.items;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public record DialogRegistryItem(String title, @Nullable Icon icon, DialogBuilder dialogBuilder) {
    public DialogRegistryItem {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Dialog title is required");
        }
        if (dialogBuilder == null) {
            throw new IllegalArgumentException("Dialog builder is required");
        }
    }

    @FunctionalInterface
    public interface DialogBuilder {
        JDialog build(JFrame parent);
    }
}