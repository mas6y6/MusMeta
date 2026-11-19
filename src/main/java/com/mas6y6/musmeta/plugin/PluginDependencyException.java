package com.mas6y6.musmeta.plugin;

public class PluginDependencyException extends PluginException {

    public PluginDependencyException(String message) {
        super(message);
    }

    public PluginDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}