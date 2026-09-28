package com.mas6y6.musmeta.config;


public interface ConfigSerializable {
    void serialize(ConfigBuilder builder);

    default void serialize(SubConfig config) {
        serialize(new ConfigBuilder(config));
    }
}
