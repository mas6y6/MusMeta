package com.mas6y6.musmeta.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mas6y6.musmeta.ui.components.MainAppFrame;
import com.mas6y6.musmeta.ui.album.AlbumPaneUI;
import com.mas6y6.musmeta.utils.ColorWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends MainAppFrame {
    public static final Logger LOGGER = LoggerFactory.getLogger(MainWindow.class.getName());
    public static final MainWindow INSTANCE = new MainWindow();

    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.NORTH);

    private MainWindow() {
        initWindow();

        setLocationRelativeTo(null);
    }

    @Override
    public boolean onClose(JFrame frame) {
        LOGGER.debug("Closing main window");

        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                frame,
                "Do you want to close MusMeta?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION
        );
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

        tabs.putClientProperty( FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true );
        tabs.putClientProperty( "JTabbedPane.tabCloseToolTipText", "Close" );
        tabs.putClientProperty( "JTabbedPane.tabCloseCallback",
            (java.util.function.BiConsumer<JTabbedPane, Integer>) (tabbedPane, tabIndex) -> {
                if (tabIndex == 0) {
                    closeThisWindow();
                } else {
                    tabbedPane.removeTabAt( tabIndex );
                }
            }
        );
        add(tabs);

        tabs.addTab("Library", new AlbumPaneUI());
    }
}
