package com.example.musmeta;

import com.mas6y6.musmeta.plugin.api.Plugin;

public class ExamplePlugin extends Plugin {

    @Override
    public void onBoot() {
        getContext().logger().info("ExamplePlugin booting (id={}, version={})", getContext().descriptor().id(), getContext().descriptor().version());
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