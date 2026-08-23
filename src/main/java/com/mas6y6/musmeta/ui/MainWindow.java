package com.mas6y6.musmeta.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mas6y6.musmeta.ui.components.MainAppFrame;
import com.mas6y6.musmeta.utils.ColorWrapper;

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
        getRootPane().putClientProperty(FlatClientProperties.USE_WINDOW_DECORATIONS, true);
        getRootPane().putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_HEIGHT, 30);

        JPanel titlebar = new JPanel();
        titlebar.setPreferredSize(new Dimension(getWidth(), 38));
        titlebar.setBackground(new ColorWrapper(getBackground()).darker(0.85f));
        titlebar.setLayout(new BorderLayout());
        titlebar.setBorder(BorderFactory.createEmptyBorder(
                0,
                10,
                0,
                180
        ));
        add(titlebar,BorderLayout.NORTH);

        JLabel title = new JLabel("MusMeta");
        titlebar.add(title,BorderLayout.WEST);
    }
}
