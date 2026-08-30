package com.example.musmeta;

import com.mas6y6.musmeta.plugin.api.Plugin;
import com.mas6y6.musmeta.registry.Registries;
import com.mas6y6.musmeta.registry.base.SettingTab;
import com.mas6y6.musmeta.registry.base.SettingsTabComponentBuilder;

import javax.swing.*;

public class ExamplePlugin extends Plugin {

    @Override
    public void onBoot() {
        getContext().logger().info("ExamplePlugin booting (id={}, version={})", getContext().descriptor().id(), getContext().descriptor().version());

        Registries.SETTING_TABS.register("exampleplugin", new SettingTab("Example Plugin", null,
            () -> new JLabel("Example Plugin"))
        );
    }

    @Override
    public void onEnable() {
        getContext().logger().info("ExamplePlugin enabled");
    }

    @Override
    public void onDisable() {
        getContext().logger().info("ExamplePlugin disabled");
    }
}