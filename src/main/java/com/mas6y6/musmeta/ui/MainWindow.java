package com.mas6y6.musmeta.ui;

import com.mas6y6.musmeta.ui.components.MainAppFrame;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends MainAppFrame {
    public static final MainWindow INSTANCE = new MainWindow();

    private MainWindow() {
        initWindow();

        setLocationRelativeTo(null);
    }

    @Override
    public boolean onClose() {
        return false;
    }

    private void initWindow() {
        JPanel panel = new JPanel();
        add(panel);
    }
}
