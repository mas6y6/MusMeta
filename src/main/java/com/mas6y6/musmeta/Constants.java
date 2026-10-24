package com.mas6y6.musmeta;

import com.mas6y6.musmeta.ui.components.MusMetaFrame;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class Constants {
    public static final Dimension PROMPT_MAX_SIZE = new Dimension(400, 300);
    public static final String APP_NAME = "MusMeta";
    public static final Image APP_ICON = new ImageIcon(
            Objects.requireNonNull(MusMetaFrame.class.getResource("/icon.png"))
    ).getImage();

    public static final Set<String> MUSIC_EXTENSIONS = Set.of(
            "mp3", "flac", "m4a", "ogg", "opus", "wav", "aiff", "aif", "ape", "wv", "tta"
    );

    public static boolean isAudioFile(Path path) {
        String filename = path.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        int dot = filename.lastIndexOf('.');

        if (dot == -1 || dot == filename.length() - 1) {
            return false;
        }

        String extension = filename.substring(dot + 1);

        return MUSIC_EXTENSIONS.contains(extension);
    }
}
