package com.mas6y6.musmeta.registry;

import com.mas6y6.musmeta.registry.base.SettingTab;

public final class Registries {
    private static boolean frozen;

    public static final Registry<String, SettingTab> SETTING_TABS = new Registry<>("SETTING_TABS");

    private Registries() {
    }

    public static void freezeAll() {
        if (frozen) return;
        frozen = true;
        for (Registry<?, ?> registry : Registry.all()) {
            registry.freeze();
        }
    }

    static boolean isFrozen() {
        return frozen;
    }
}
