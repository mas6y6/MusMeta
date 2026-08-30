package com.mas6y6.musmeta.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.SystemInfo;
import com.jthemedetecor.OsThemeDetector;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.settings.Theme;
import com.mas6y6.musmeta.ui.components.MainAppFrame;
import com.mas6y6.musmeta.ui.album.AlbumLibraryUI;
import com.mas6y6.musmeta.ui.dialogs.base.EXTDialog;
import com.mas6y6.musmeta.ui.prompts.MusicScanPrompt;
import com.mas6y6.musmeta.ui.subwindows.AboutWindow;
import com.mas6y6.musmeta.ui.subwindows.SettingsWindow;
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

    private void applyTitleBarBackground() {
        Theme theme = Settings.PREFERRED_THEME.get();
        boolean dark = theme == Theme.DARK
                || (theme == Theme.SYSTEM && OsThemeDetector.getDetector().isDark());
        getRootPane().putClientProperty(
                FlatClientProperties.TITLE_BAR_BACKGROUND,
                dark ? getBackground().darker() : getBackground()
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

        OsThemeDetector.getDetector().registerListener(isDark -> {
            if (Settings.PREFERRED_THEME.get() == Theme.SYSTEM) {
                applyTitleBarBackground();
            }
        });

        Settings.PREFERRED_THEME.addListener(theme -> applyTitleBarBackground());

        applyTitleBarBackground();

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

        //region File menu
        JMenu fileMenu = new JMenu("File");

        // Save button
        JMenuItem importsongs = new JMenuItem("Import song(s)...");
        fileMenu.add(importsongs);

        JMenuItem settings = new JMenuItem("Settings");
        settings.addActionListener(e ->
                new SettingsWindow(this).setVisible(true)
        );
        fileMenu.add(settings);

        // Exit button
        fileMenu.addSeparator();
        JMenuItem aboutItem =
                new JMenuItem("About");
        fileMenu.add(aboutItem);
        JMenuItem exitItem =
                new JMenuItem("Exit");
        fileMenu.add(exitItem);
        exitItem.addActionListener(e ->
                closeThisWindow()
        );
        aboutItem.addActionListener(e ->
                new AboutWindow(this).setVisible(true)
        );
        //endregion


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
