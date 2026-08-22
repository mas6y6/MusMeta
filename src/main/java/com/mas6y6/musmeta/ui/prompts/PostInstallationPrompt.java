package com.mas6y6.musmeta.ui.prompts;

import com.mas6y6.musmeta.MusMetaConstants;
import com.mas6y6.musmeta.ui.components.MusMetaFrame;

import javax.swing.*;
import java.awt.*;

public class PostInstallationPrompt extends MusMetaFrame {
    private static Dimension SIZE = new Dimension(800, 600);
    public PostInstallationPrompt() {
        setSubTitle("Post Installation");
        initWindow();
        setVisible(true);
    }

    private void initWindow() {
        setMinimumSize(SIZE);
        setSize(SIZE);
    }
}
