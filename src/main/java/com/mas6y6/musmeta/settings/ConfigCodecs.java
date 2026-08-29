package com.mas6y6.musmeta.settings;

import com.mas6y6.musmeta.config.ConfigCodec;
import com.mas6y6.musmeta.config.ConfigManager;

import java.nio.file.Path;

public class ConfigCodecs {
    public static ConfigCodec<Path> PATH_CODEC = ConfigCodec.of(
            (type, builder) -> {
                builder.set("path", type.toString());
            },
            (builder) -> Path.of(builder.getString("path"))
    );

    public static void register() {
        ConfigManager.registerCodec(Path.class,PATH_CODEC);
    }
}
