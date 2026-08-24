package com.mas6y6.musmeta.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.mas6y6.musmeta.Constants.APP_ICON;

public abstract class MainAppFrame extends JFrame {

    public MainAppFrame() {
        super("MusMeta");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setIconImage(APP_ICON);

        setMinimumSize(new Dimension(1000, 600));
        setSize(1000, 600);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeThisWindow();
            }
        });
    }

    public void closeThisWindow() {
        if (onClose(this)) {
            dispose();
        }
    }

    public abstract boolean onClose(JFrame frame);
}