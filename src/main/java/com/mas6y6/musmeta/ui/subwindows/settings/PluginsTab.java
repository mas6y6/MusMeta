package com.mas6y6.musmeta.ui.subwindows.settings;

import com.mas6y6.musmeta.Bootstrap;
import com.mas6y6.musmeta.plugin.PluginManager;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.subwindows.settings.base.SettingTab;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PluginsTab extends SettingTab {

    private static final String[] COLUMNS = {"Enabled", "Plugin Name"};

    private final DefaultTableModel model;
    private final List<String> rowIds = new ArrayList<>();
    private boolean edited = false     ;
    private boolean closePromptHooked = false;

    public PluginsTab() {
        super();

        JLabel title = new JLabel("Plugins");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        CONTENT.add(title);

        CONTENT.add(Box.createVerticalStrut(5));

        JLabel description = new JLabel(
                "Configure which plugins are enabled."
        );
        description.setForeground(UIManager.getColor("Label.disabledForeground"));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        CONTENT.add(description);

        Set<String> disabledIds = new HashSet<>(Settings.DISABLED_PLUGINS.get());

        model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        PluginManager.getInstance().containers().forEach(container -> {
            String id = container.descriptor().id();
            String name = container.descriptor().name();
            boolean enabled = !disabledIds.contains(id);
            rowIds.add(id);
            model.addRow(new Object[]{enabled, name});
        });

        disabledIds.forEach(id -> {
            if (!rowIds.contains(id)) {
                rowIds.add(id);
                model.addRow(new Object[]{false, id});
            }
        });

        model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                edited = true;
                updateDisabledPlugins();
            }
        });

        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(400, 150));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        CONTENT.add(Box.createVerticalStrut(5));
        CONTENT.add(scrollPane);

        scrollPane.setVisible(!Bootstrap.isSkipBootstrap());
        CONTENT.add(new JLabel("""
<html>
<strong>MusMeta Bootstrap is disabled!</strong><br/>
Meaning that all plugins that are in the plugins folder have not been loaded!<br/>
<br/>
To fix this please check your startup parameters!
</html>"""){{
            this.setAlignmentX(Component.LEFT_ALIGNMENT);
            this.setVisible(Bootstrap.isSkipBootstrap());
        }});
    }

    private void updateDisabledPlugins() {
        List<String> disabled = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (!Boolean.TRUE.equals(model.getValueAt(i, 0))) {
                disabled.add(rowIds.get(i));
            }
        }
        disabled.sort(String::compareTo);
        Settings.DISABLED_PLUGINS.set(disabled);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        hookClosePrompt();
    }

    private void hookClosePrompt() {
        if (closePromptHooked) {
            return;
        }
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null) {
            return;
        }
        closePromptHooked = true;
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (edited) {
                    JOptionPane.showMessageDialog(
                            window,
                            "Plugin changes will only take effect after you restart MusMeta.",
                            "Plugins changed",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });
    }
}
