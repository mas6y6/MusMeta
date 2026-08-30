package com.mas6y6.musmeta.registry;

import com.mas6y6.musmeta.registry.base.SettingTab;

public class Registries {
    public static final Registry<String, SettingTab> SETTING_TABS = new Registry<>("SETTING_TABS");

    public static void freeze() {
        SETTING_TABS.freeze();
    }
}
