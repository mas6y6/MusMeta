package com.mas6y6.musmeta.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.SystemInfo;
import com.mas6y6.musmeta.ui.components.MainAppFrame;
import com.mas6y6.musmeta.ui.album.AlbumLibraryUI;
import com.mas6y6.musmeta.ui.dialogs.base.EXTDialog;
import com.mas6y6.musmeta.ui.prompts.MusicScanPrompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends MainAppFrame {
    public static final Logger LOGGER =
            LoggerFactory.getLogger(MainWindow.class.getName());

    public static final MainWindow INSTANCE = new MainWindow();

    private final JTabbedPane tabs =
            new JTabbedPane(SwingConstants.TOP);

    private MainWindow() {
        if (SystemInfo.isMacOS) {
            if( SystemInfo.isMacFullWindowContentSupported ) {
                getRootPane().putClientProperty( "apple.awt.fullWindowContent", true );
                getRootPane().putClientProperty( "apple.awt.transparentTitleBar", true );
                getRootPane().putClientProperty( "apple.awt.windowTitleVisible", false );
                getRootPane().putClientProperty( FlatClientProperties.MACOS_WINDOW_BUTTONS_SPACING,
                        FlatClientProperties.MACOS_WINDOW_BUTTONS_SPACING_LARGE );
            }
        }
        initWindow();

        setLocationRelativeTo(null);
    }

    @Override
    public boolean onClose(JFrame frame) {
        LOGGER.debug("Closing main window");

        return JOptionPane.YES_OPTION == EXTDialog.showConfirmDialog(
                frame,
                "Do you want to close MusMeta?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION
        );
    }

    private void initWindow() {
        //region Window Decorations

        getRootPane().putClientProperty(
                FlatClientProperties.USE_WINDOW_DECORATIONS,
                true
        );

        getRootPane().putClientProperty(
                FlatClientProperties.MENU_BAR_EMBEDDED,
                true
        );

        getRootPane().putClientProperty(
                FlatClientProperties.FULL_WINDOW_CONTENT,
                false
        );

        getRootPane().putClientProperty(
                FlatClientProperties.TITLE_BAR_HEIGHT,
                38
        );

        getRootPane().putClientProperty(
                FlatClientProperties.TITLE_BAR_BACKGROUND,
                getBackground().darker()
        );

        //endregion


        //region Menu Bar

        JMenuBar menuBar = new JMenuBar();

        if (SystemInfo.isMacOS) {
            Dimension dim = menuBar.getPreferredSize();
            dim.height = 50;
            menuBar.setPreferredSize(dim);

            menuBar.add(Box.createHorizontalStrut( 100 ), 0);
        }
        menuBar.setOpaque(false);
        menuBar.setBorder(
                BorderFactory.createEmptyBorder()
        );

        // File

        JMenu fileMenu = new JMenu("File");

        fileMenu.add(
                new JMenuItem("Open")
        );

        fileMenu.add(
                new JMenuItem("Save")
        );

        fileMenu.addSeparator();

        JMenuItem exitItem =
                new JMenuItem("Exit");

        exitItem.addActionListener(e ->
                closeThisWindow()
        );

        fileMenu.add(exitItem);


        // Edit

        JMenu editMenu =
                new JMenu("Edit");

        // View

        JMenu viewMenu =
                new JMenu("View");

        // Library

        JMenu libraryMenu =
                new JMenu("Library");

        JMenuItem musicScanMenuItem =
                new JMenuItem("Music Scan");
        musicScanMenuItem.addActionListener(e ->
                new MusicScanPrompt(this).setVisible(true)
        );

        libraryMenu.add(musicScanMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(libraryMenu);
        menuBar.add(viewMenu);

        /*
         * This is what tells Swing/FlatLaf that this is
         * the window's menu bar.
         *
         * FlatLaf then embeds it into its title pane.
         */
        setJMenuBar(menuBar);

        //endregion


        //region Tabs

        tabs.putClientProperty(
                FlatClientProperties.TABBED_PANE_TAB_CLOSABLE,
                true
        );

        tabs.putClientProperty(
                "JTabbedPane.tabCloseToolTipText",
                "Close"
        );

        tabs.putClientProperty(
                "JTabbedPane.tabCloseCallback",
                (java.util.function.BiConsumer<JTabbedPane, Integer>)
                        (tabbedPane, tabIndex) -> {

                            if (tabIndex == 0) {
                                closeThisWindow();
                            } else {
                                tabbedPane.removeTabAt(tabIndex);
                            }
                        }
        );

        tabs.addTab(
                "Library",
                new AlbumLibraryUI()
        );

        add(
                tabs,
                BorderLayout.CENTER
        );

        //endregion
    }
}
