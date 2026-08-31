package com.mas6y6.musmeta;

import com.mas6y6.musmeta.ui.components.MusMetaFrame;

import javax.swing.*;
import java.awt.*;
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
}
