package com.mas6y6.musmeta.ui.subwindows;

import com.mas6y6.musmeta.registry.Registries;
import com.mas6y6.musmeta.ui.subwindows.settings.AppearanceTab;
import com.mas6y6.musmeta.ui.subwindows.settings.FFmpegSettingsTab;
import com.mas6y6.musmeta.ui.subwindows.settings.MusicTab;
import com.mas6y6.musmeta.ui.subwindows.settings.PluginsTab;
import com.mas6y6.musmeta.ui.subwindows.settings.UpdatesTab;

import javax.swing.*;
import java.awt.*;

public class SettingsWindow extends JDialog {
    private static final Dimension DIALOG_SIZE = new Dimension(800, 500);
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);

    public SettingsWindow(JFrame parentWindow) {
        super(parentWindow, "Settings", true);
        setSize(DIALOG_SIZE);
        setMinimumSize(DIALOG_SIZE);
        setLocationRelativeTo(getOwner());

        initTabs();
    }

    private void initTabs() {
        tabs.putClientProperty("JTabbedPane.tabHeight", 30);
        tabs.putClientProperty("JTabbedPane.minimumTabWidth", 100);

        tabs.addTab("Appearance", new AppearanceTab());
        tabs.addTab("Music", new MusicTab());
        tabs.addTab("FFmpeg", new FFmpegSettingsTab());
        tabs.addTab("Updates", new UpdatesTab());
        tabs.addTab("Plugins", new PluginsTab());

        Registries.SETTING_TABS.getAll().forEach(tab -> {
            if (tab.getValue().icon() != null) {
                tabs.addTab(tab.getKey(), tab.getValue().icon(), tab.getValue().settingsTabBuilder().build());
            } else {
                tabs.addTab(tab.getKey(), tab.getValue().settingsTabBuilder().build());
            }
        });

        add(tabs, BorderLayout.CENTER);
    }
}