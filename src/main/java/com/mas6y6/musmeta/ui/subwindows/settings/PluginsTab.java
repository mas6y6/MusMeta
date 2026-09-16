package com.mas6y6.musmeta.ui.subwindows.settings;

import com.mas6y6.musmeta.ui.subwindows.settings.base.SettingTab;

import javax.swing.*;
import java.awt.*;

public class PluginsTab extends SettingTab {
    public PluginsTab() {
        super();

        String[] columns = {"Enabled", "Plugin Name"};
        Object[][] data = {
                {true, "Plugin 1"}
        };

        JTable table = new JTable(data, columns);

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        table.setRowHeight(30);
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);

        CONTENT.add(scrollPane);
    }
}