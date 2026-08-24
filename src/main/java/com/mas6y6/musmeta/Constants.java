package com.mas6y6.musmeta;

import com.mas6y6.musmeta.ui.components.MusMetaFrame;

import javax.swing.*;
import java.awt.*;

public class Constants {
    public static final Dimension PROMPT_MAX_SIZE = new Dimension(400, 300);
    public static final String APP_NAME = "MusMeta";
    public static final Image APP_ICON = new ImageIcon(
            MusMetaFrame.class.getResource("/icon.png")
    ).getImage();
}
