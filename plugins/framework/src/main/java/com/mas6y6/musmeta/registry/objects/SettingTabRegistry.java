package com.mas6y6.musmeta.registry.objects;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public record SettingTabRegistry(String name, @Nullable Icon icon, SettingsTabComponentBuilder settingsTabBuilder) {
}

