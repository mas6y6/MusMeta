package com.mas6y6.musmeta.ui.subwindows.settings.base;

import javax.swing.*;
import java.awt.*;

public abstract class SettingTab extends JPanel {
    protected final JPanel CONTENT = new JPanel();

    protected SettingTab() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        CONTENT.setLayout(new BoxLayout(CONTENT, BoxLayout.Y_AXIS));

        add(CONTENT, BorderLayout.NORTH);
    }
}
