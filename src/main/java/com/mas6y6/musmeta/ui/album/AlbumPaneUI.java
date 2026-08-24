package com.mas6y6.musmeta.ui.album;

import javax.swing.*;
import java.awt.*;

public class AlbumPaneUI extends JPanel {
    private static final int GAP = 15;
    private static final int PADDING = 10;

    public AlbumPaneUI() {
        setLayout(new FlowLayout(
                FlowLayout.LEFT,
                GAP,
                GAP
        ));

        setBorder(BorderFactory.createEmptyBorder(
                PADDING,
                PADDING,
                PADDING,
                PADDING
        ));

        add(new AlbumUI("test","test"));
    }
}