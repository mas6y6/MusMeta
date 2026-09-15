package com.mas6y6.musmeta.ui.subwindows.settings;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.components.PathField;
import com.mas6y6.musmeta.ui.subwindows.settings.base.SettingTab;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MusicTab extends SettingTab {
    public MusicTab() {
        super();

        JLabel title = new JLabel("Music");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        CONTENT.add(title);

        CONTENT.add(new JLabel("Music Directory"));

        CONTENT.add(Box.createVerticalStrut(5));

        PathField musicPathField = new PathField(this);
        musicPathField.setAlignmentX(Component.LEFT_ALIGNMENT);
        musicPathField.setPath(Settings.MUSIC_DIRECTORY_PATH.get().toString());

        musicPathField.addPropertyChangeListener("path", evt -> {
            Settings.MUSIC_DIRECTORY_PATH.set(
                    Paths.get((String) evt.getNewValue())
            );
        });

        CONTENT.add(musicPathField);

        CONTENT.add(Box.createVerticalStrut(10));

        JSeparator separator = new JSeparator();
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        CONTENT.add(separator);

        CONTENT.add(Box.createVerticalStrut(10));
        CONTENT.add(new JLabel("Target format for auto-conversion during scan"));
        CONTENT.add(Box.createVerticalStrut(10));

        List<AlbumFormatNormalizer.AudioFormat> formats = AlbumFormatNormalizer.allFormats();
        JComboBox<AlbumFormatNormalizer.AudioFormat> formatPicker =
                new JComboBox<>(formats.toArray(new AlbumFormatNormalizer.AudioFormat[0]));
        formatPicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        formatPicker.setSelectedItem(
                AlbumFormatNormalizer.fromSetting(Settings.AUDIO_TARGET_FORMAT.get())
        );
        formatPicker.addActionListener(e -> {
            Object selected = formatPicker.getSelectedItem();
            if (selected instanceof AlbumFormatNormalizer.AudioFormat format) {
                Settings.AUDIO_TARGET_FORMAT.set(format.extension());
            }
        });
        CONTENT.add(formatPicker);

        CONTENT.add(Box.createVerticalStrut(10));
        CONTENT.add(new JLabel("Incompatible songs are converted into <music directory>/MusMeta."));
        CONTENT.add(Box.createVerticalStrut(10));

        CONTENT.add(Box.createVerticalStrut(10));
        CONTENT.add(new JLabel("Ignored paths for music scan"));
        CONTENT.add(Box.createVerticalStrut(10));

        List<Path> ignoredPaths = Settings.MUSIC_SCAN_IGNORE_PATHS.get();

        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"Ignored paths"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        for (Path path : ignoredPaths) {
            model.addRow(new Object[]{path.toString()});
        }

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);

        Runnable saveIgnoredPaths = () -> {
            List<Path> paths = new ArrayList<>();

            for (int i = 0; i < model.getRowCount(); i++) {
                String value = (String) model.getValueAt(i, 0);

                if (value != null && !value.isBlank()) {
                    paths.add(Paths.get(value));
                }
            }

            Settings.MUSIC_SCAN_IGNORE_PATHS.set(paths);
        };

        JButton addButton = new JButton("+");

        addButton.addActionListener(e -> {
            model.addRow(new Object[]{""});

            int row = model.getRowCount() - 1;

            table.setRowSelectionInterval(row, row);
            table.editCellAt(row, 0);

            saveIgnoredPaths.run();
        });

        JButton removeButton = new JButton("-");

        removeButton.addActionListener(e -> {
            int row = table.getSelectedRow();

            if (row != -1) {
                model.removeRow(row);
                saveIgnoredPaths.run();
            }
        });

        JButton browseButton = new JButton("+ Browse...");

        browseButton.addActionListener(e -> {
            SystemFileChooser chooser = new SystemFileChooser();

            chooser.setDialogTitle("Select ignored directory");
            chooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(table) == SystemFileChooser.APPROVE_OPTION) {
                Path path = chooser.getSelectedFile()
                        .toPath()
                        .toAbsolutePath()
                        .normalize();

                for (int i = 0; i < model.getRowCount(); i++) {
                    if (path.toString().equals(model.getValueAt(i, 0))) {
                        return;
                    }
                }

                model.addRow(new Object[]{path.toString()});
                saveIgnoredPaths.run();
            }
        });

        model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                saveIgnoredPaths.run();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        buttons.add(addButton);
        buttons.add(removeButton);
        buttons.add(browseButton);

        JPanel ignoredPathsPanel = new JPanel(new BorderLayout(0, 8));
        ignoredPathsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(400, 150));

        ignoredPathsPanel.add(scrollPane, BorderLayout.CENTER);
        ignoredPathsPanel.add(buttons, BorderLayout.SOUTH);

        CONTENT.add(ignoredPathsPanel);
    }
}