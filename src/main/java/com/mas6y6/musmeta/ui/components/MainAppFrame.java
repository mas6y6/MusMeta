package com.mas6y6.musmeta.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public abstract class MainAppFrame extends JFrame {
    public MainAppFrame() {
        super("MusMeta");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 600));
        setSize(1000, 600);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (onClose()) {
                    dispose();
                }
            }
        });
    }

    public abstract boolean onClose();
}