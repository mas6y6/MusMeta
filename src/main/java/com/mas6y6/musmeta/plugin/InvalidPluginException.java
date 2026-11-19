package com.mas6y6.musmeta.plugin;

public class InvalidPluginException extends PluginException {

    public InvalidPluginException(String message) {
        super(message);
    }

    public InvalidPluginException(String message, Throwable cause) {
        super(message, cause);
    }
}