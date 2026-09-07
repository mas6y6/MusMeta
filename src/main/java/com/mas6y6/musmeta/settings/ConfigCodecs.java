package com.mas6y6.musmeta.settings;

import com.mas6y6.musmeta.config.ConfigCodec;
import com.mas6y6.musmeta.config.ConfigManager;
import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Disc;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;

import java.nio.file.Path;

public class ConfigCodecs {
    public static ConfigCodec<Path> PATH_CODEC = ConfigCodec.of(
            (type, builder) -> {
                builder.set("path", type.toString());
            },
            (builder) -> Path.of(builder.getString("path"))
    );

    public static void register() {
        ConfigManager.registerCodec(Path.class, PATH_CODEC);
        ConfigManager.registerCodec(Song.class, Song.CODEC);
        ConfigManager.registerCodec(Disc.class, Disc.CODEC);
        ConfigManager.registerCodec(Album.class, Album.CODEC);
        ConfigManager.registerCodec(Library.class, Library.CODEC);
    }
}
