package com.mas6y6.musmeta.registry.base;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public record SettingTab(String name, @Nullable Icon icon, SettingsTabComponentBuilder settingsTabBuilder) {
}

