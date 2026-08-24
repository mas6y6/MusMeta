package com.mas6y6.musmeta.ui.components;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static com.mas6y6.musmeta.Constants.APP_ICON;
import static com.mas6y6.musmeta.Constants.APP_NAME;

public class MusMetaFrame extends JFrame {
    public MusMetaFrame() {
        super("MusMeta");
        setIconImage(APP_ICON);
    }

    public void setSubTitle(@Nullable String title) {
        if (title != null) {
            setTitle(APP_NAME + " - " + title);
        } else {
            setTitle(APP_NAME);
        }
    }
}