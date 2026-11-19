package com.mas6y6.musmeta.registry.items;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public record SettingTabRegistryItem(String name, @Nullable Icon icon, SettingsTabComponentBuilder settingsTabBuilder) {
}

