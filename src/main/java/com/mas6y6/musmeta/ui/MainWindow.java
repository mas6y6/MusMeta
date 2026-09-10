package com.mas6y6.musmeta.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.SystemInfo;
import com.jthemedetecor.OsThemeDetector;
import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.registry.Registries;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.settings.Theme;
import com.mas6y6.musmeta.ui.components.MainAppFrame;
import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.ui.components.MusicPlayerPanel;
import com.mas6y6.musmeta.ui.dialogs.ProcessMusicDialog;
import com.mas6y6.musmeta.ui.tabs.AlbumDetailUI;

import java.io.File;
import com.mas6y6.musmeta.ui.album.LibraryUI;
import com.mas6y6.musmeta.ui.dialogs.base.EXTDialog;
import com.mas6y6.musmeta.ui.dialogs.MusicScanDialog;
import com.mas6y6.musmeta.ui.subwindows.AboutWindow;
import com.mas6y6.musmeta.ui.subwindows.SettingsWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainWindow extends MainAppFrame {
    public static final Logger LOGGER =
            LoggerFactory.getLogger(MainWindow.class.getName());

    public static final MainWindow INSTANCE = new MainWindow();

    private final JTabbedPane tabs = new JTabbedPane(SwingConstants.TOP);
    private final MusicPlayerPanel musicPlayer = new MusicPlayerPanel();
    private final JToolBar toolbar = new JToolBar();

    private List<Song> selectedSongs = List.of();
    private LibraryUI libraryUI = new LibraryUI();

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

    private void registerMacAppMenuHandlers() {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler(e ->
                    new AboutWindow(this).setVisible(true)
            );
        }
        if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
            desktop.setPreferencesHandler(e ->
                    new SettingsWindow(this).setVisible(true)
            );
        }
        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((e, response) -> {
                if (onClose(this)) {
                    response.performQuit();
                } else {
                    response.cancelQuit();
                }
            });
        }
    }

    /**
     * Opens the given album in an iTunes-style detail tab, or activates the
     * existing tab for that album if it is already open.
     */
    public void openAlbumTab(Album album) {
        SwingUtilities.invokeLater(() -> {
            int index = tabs.indexOfTab(album.getTitle());
            if (index >= 0) {
                tabs.setSelectedIndex(index);
                return;
            }
            AlbumDetailUI detail = new AlbumDetailUI(album);
            detail.setSelectionListener(this::setSelectedSongs);
            tabs.addTab(album.getTitle(), detail);
            tabs.setSelectedIndex(tabs.getTabCount() - 1);
        });
    }

    private void setSelectedSongs(List<Song> songs) {
        selectedSongs = songs != null ? songs : List.of();
    }

    public void updateAlbumTabs(Album album, String oldTitle) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                Component comp = tabs.getComponentAt(i);
                if (comp instanceof AlbumDetailUI detail) {
                    if (detail.getAlbum() == album || (oldTitle != null && oldTitle.equalsIgnoreCase(tabs.getTitleAt(i)))) {
                        tabs.setTitleAt(i, album.getTitle());
                        detail.refresh();
                    }
                }
            }
        });
    }

    public void refreshAllDetailTabs() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                Component comp = tabs.getComponentAt(i);
                if (comp instanceof AlbumDetailUI detail) {
                    tabs.setTitleAt(i, detail.getAlbum().getTitle());
                    detail.refresh();
                }
            }
        });
    }

    /**
     * Re-reads the selection of the currently visible tab so the Selection
     * menu mirrors whichever tab is active.
     */
    private void refreshSelectionForActiveTab() {
        Component tab = tabs.getSelectedComponent();
        if (tab instanceof AlbumDetailUI detail) {
            setSelectedSongs(detail.getSelectedSongs());
        } else {
            setSelectedSongs(List.of());
        }
    }

    public List<Song> getSelectedSongs() {
        return List.copyOf(selectedSongs);
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

        menuBar.setOpaque(false);
        menuBar.setBorder(
                BorderFactory.createEmptyBorder()
        );

        // File

        //region File menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem importsongs = new JMenuItem("Import song(s)...");
        importsongs.addActionListener((e) -> {
            var fsc = new SystemFileChooser(System.getProperty("user.home"));
            fsc.setMultiSelectionEnabled(true);
            fsc.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter("Audio Files", Constants.MUSIC_EXTENSIONS.toArray(String[]::new)));
            fsc.setAcceptAllFileFilterUsed(false);
            if (fsc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fsc.getSelectedFiles();
                if (selectedFiles == null || selectedFiles.length == 0) {
                    File single = fsc.getSelectedFile();
                    if (single != null) {
                        selectedFiles = new File[]{single};
                    }
                }
                if (selectedFiles != null && selectedFiles.length > 0) {
                    var dialog = new ProcessMusicDialog(this, selectedFiles);
                    dialog.startAndShow();
                }
            }
            getLibraryUI().refresh();
        });

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

        if (SystemInfo.isMacOS) {
            aboutItem.setVisible(false);
            exitItem.setVisible(false);
            registerMacAppMenuHandlers();
        }
        //endregion

        //region

        // Edit

        JMenu editMenu =
                new JMenu("Edit");

        JMenuItem getInfoItem = new JMenuItem("Get Info");
        getInfoItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        getInfoItem.addActionListener(e -> {
            if (!selectedSongs.isEmpty()) {
                new com.mas6y6.musmeta.ui.dialogs.EditSongDialog(this, selectedSongs).setVisible(true);
            } else {
                Component tab = tabs.getSelectedComponent();
                if (tab instanceof AlbumDetailUI detail) {
                    new com.mas6y6.musmeta.ui.dialogs.EditAlbumDialog(this, detail.getAlbum()).setVisible(true);
                }
            }
        });
        editMenu.add(getInfoItem);

        JMenuItem editAlbumItem = new JMenuItem("Edit Album Info...");
        editAlbumItem.addActionListener(e -> {
            Component tab = tabs.getSelectedComponent();
            if (tab instanceof AlbumDetailUI detail) {
                new com.mas6y6.musmeta.ui.dialogs.EditAlbumDialog(this, detail.getAlbum()).setVisible(true);
            }
        });
        editMenu.add(editAlbumItem);

        editMenu.addSeparator();

        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        selectAllItem.addActionListener(e -> {
            Component tab = tabs.getSelectedComponent();
            if (tab instanceof AlbumDetailUI detail) {
                detail.selectAllTracks();
            }
        });
        editMenu.add(selectAllItem);

        JMenuItem deselectAllItem = new JMenuItem("Deselect All");
        deselectAllItem.addActionListener(e -> {
            Component tab = tabs.getSelectedComponent();
            if (tab instanceof AlbumDetailUI detail) {
                detail.deselectAllTracks();
            }
        });
        editMenu.add(deselectAllItem);



        // View

        JMenu viewMenu =
                new JMenu("View");

        JCheckBoxMenuItem showPlayerItem = new JCheckBoxMenuItem("Show Music Player");
        showPlayerItem.addActionListener(e -> {
            Settings.SHOW_MUSIC_PLAYER.set(showPlayerItem.isSelected());
            musicPlayer.setVisible(showPlayerItem.isSelected());
        });

        showPlayerItem.setSelected(Settings.SHOW_MUSIC_PLAYER.get());

        viewMenu.add(showPlayerItem);

        // Library

        JMenu libraryMenu =
                new JMenu("Library");

        JMenuItem musicScanMenuItem =
                new JMenuItem("Music Scan");
        musicScanMenuItem.addActionListener(e ->
                new MusicScanDialog(this).setVisible(true)
        );

        libraryMenu.add(musicScanMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(libraryMenu);
        menuBar.add(viewMenu);

        Registries.MENU_ITEMS.getAll().forEach(e -> menuBar.add(e.getValue()));

        /*
         * This is what tells Swing/FlatLaf that this is
         * the window's menu bar.
         *
         * FlatLaf then embeds it into its title pane.
         */
        setJMenuBar(menuBar);

        //endregion


        //region Title Bar / Toolbar

        toolbar.setFloatable(false);
        toolbar.setOpaque(true);
        toolbar.setBorder(
                BorderFactory.createEmptyBorder()
        );

        Dimension titleBarSize = new Dimension(0, SystemInfo.isMacOS ? 48 : 38);
        toolbar.setPreferredSize(titleBarSize);
        toolbar.setMinimumSize(titleBarSize);
        toolbar.setBackground(getBackground().darker());
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, SystemInfo.isMacOS ? 48 : 38));

        if (SystemInfo.isMacOS) {
            toolbar.add(Box.createHorizontalStrut(80));
        }

        add(toolbar, BorderLayout.NORTH);

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
                                refreshSelectionForActiveTab();
                            }
                        }
        );

        tabs.putClientProperty("TabbedPane.tabLayoutPolicy", "scroll");

        tabs.addChangeListener(e -> refreshSelectionForActiveTab());

        tabs.setTabLayoutPolicy(
                JTabbedPane.SCROLL_TAB_LAYOUT
        );

        tabs.addTab(
                "Library",
                libraryUI
        );

        add(
                tabs,
                BorderLayout.CENTER
        );

        add(
                musicPlayer,
                BorderLayout.SOUTH
        );

        //endregion
    }

    public Component getSelectedTab() {
        return tabs.getSelectedComponent();
    }
    public LibraryUI getLibraryUI() {
        return libraryUI;
    }
    public JToolBar getToolbar() {
        return toolbar;
    }
}
