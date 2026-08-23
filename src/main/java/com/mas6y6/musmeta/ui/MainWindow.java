package com.mas6y6.musmeta.ui;

import com.mas6y6.musmeta.ui.components.MusMetaFrame;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends MusMetaFrame {
    public static final MainWindow INSTANCE = new MainWindow();

    private MainWindow() {
        initWindow();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initWindow() {
        setMinimumSize(new Dimension(1000, 600));
        setSize(1000, 600);
    }
}
