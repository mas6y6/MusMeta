package com.mas6y6.musmeta.plugin.api;

public abstract class Plugin {

    private PluginContext context;
    private PluginState state = PluginState.LOADED;

    public void onBoot() throws Exception {
    }

    public void onEnable() throws Exception {
    }

    public void onDisable() throws Exception {
    }

    public final PluginContext getContext() {
        return context;
    }

    public final PluginState getState() {
        return state;
    }

    public final void setContext(PluginContext context) {
        this.context = context;
    }

    public final void setState(PluginState state) {
        this.state = state;
    }
}