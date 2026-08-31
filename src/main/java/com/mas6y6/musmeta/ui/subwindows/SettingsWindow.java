package com.mas6y6.musmeta.ui.subwindows;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.jthemedetecor.OsThemeDetector;
import com.mas6y6.musmeta.registry.Registries;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.settings.Theme;
import com.mas6y6.musmeta.ui.components.PathField;
import com.mas6y6.musmeta.ui.subwindows.settings.FFmpegSettingsTab;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SettingsWindow extends JDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsWindow.class);
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

        tabs.addTab("Appearance", appearanceTab());
        tabs.addTab("Music", musicTab());
        tabs.addTab("FFmpeg", ffmpegTab());

        Registries.SETTING_TABS.getAll().forEach(tab -> {
            if (tab.getValue().icon() != null) {
                tabs.addTab(tab.getKey(), tab.getValue().icon(), tab.getValue().settingsTabBuilder().build());
            } else {
                tabs.addTab(tab.getKey(), tab.getValue().settingsTabBuilder().build());
            }
        });

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel appearanceTab() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Appearance");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        content.add(title);
        content.add(Box.createVerticalStrut(20));

        JRadioButton systemDefaultBtn =
                new JRadioButton("Automatic with system");
        JRadioButton lightBtn =
                new JRadioButton("Light");
        JRadioButton darkBtn =
                new JRadioButton("Dark");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(systemDefaultBtn);
        buttonGroup.add(lightBtn);
        buttonGroup.add(darkBtn);

        content.add(systemDefaultBtn);
        content.add(Box.createVerticalStrut(8));
        content.add(lightBtn);
        content.add(Box.createVerticalStrut(8));
        content.add(darkBtn);

        systemDefaultBtn.addActionListener(
                e -> {
                    Settings.PREFERRED_THEME.set(Theme.SYSTEM);
                    try {
                        if (OsThemeDetector.getDetector().isDark()) {
                            FlatAnimatedLafChange.showSnapshot();
                            UIManager.setLookAndFeel(new FlatDarkLaf());
                            FlatLaf.updateUI();
                            FlatAnimatedLafChange.hideSnapshotWithAnimation();
                        } else {
                            FlatAnimatedLafChange.showSnapshot();
                            UIManager.setLookAndFeel(new FlatLightLaf());
                            FlatLaf.updateUI();
                            FlatAnimatedLafChange.hideSnapshotWithAnimation();
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Failed to change theme", ex);
                    }
                }
        );

        lightBtn.addActionListener(
                e -> {
                    Settings.PREFERRED_THEME.set(Theme.LIGHT);
                }
        );

        darkBtn.addActionListener(
                e -> {
                    Settings.PREFERRED_THEME.set(Theme.DARK);
                }
        );

        if (Settings.PREFERRED_THEME.get() == Theme.SYSTEM) {
            systemDefaultBtn.setSelected(true);
        } else if (Settings.PREFERRED_THEME.get() == Theme.LIGHT) {
            lightBtn.setSelected(true);
        } else {
            darkBtn.setSelected(true);
        }

        page.add(content, BorderLayout.NORTH);

        return page;
    }

    private JPanel musicTab() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Music");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);

        content.add(new JLabel("Music Directory"));

        content.add(Box.createVerticalStrut(5));

        PathField musicPathField = new PathField(this);
        musicPathField.setAlignmentX(Component.LEFT_ALIGNMENT);
        musicPathField.setPath(Settings.MUSIC_DIRECTORY_PATH.get().toString());

        musicPathField.addPropertyChangeListener("path", evt -> {
            Settings.MUSIC_DIRECTORY_PATH.set(
                    Paths.get((String) evt.getNewValue())
            );
        });

        content.add(musicPathField);

        content.add(Box.createVerticalStrut(10));

        JSeparator separator = new JSeparator();
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(separator);

        content.add(Box.createVerticalStrut(10));
        content.add(new JLabel("Target format for auto-conversion during scan"));
        content.add(Box.createVerticalStrut(10));

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
        content.add(formatPicker);

        content.add(Box.createVerticalStrut(10));
        content.add(new JLabel("Incompatible songs are converted into <music directory>/MusMeta."));
        content.add(Box.createVerticalStrut(10));

        content.add(Box.createVerticalStrut(10));
        content.add(new JLabel("Ignored paths for music scan"));
        content.add(Box.createVerticalStrut(10));

        // Load ignored paths from settings
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

        // Populate table
        for (Path path : ignoredPaths) {
            model.addRow(new Object[]{path.toString()});
        }

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);

        // Save table contents to settings
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

        // Add
        JButton addButton = new JButton("+");

        addButton.addActionListener(e -> {
            model.addRow(new Object[]{""});

            int row = model.getRowCount() - 1;

            table.setRowSelectionInterval(row, row);
            table.editCellAt(row, 0);

            saveIgnoredPaths.run();
        });

        // Remove
        JButton removeButton = new JButton("-");

        removeButton.addActionListener(e -> {
            int row = table.getSelectedRow();

            if (row != -1) {
                model.removeRow(row);
                saveIgnoredPaths.run();
            }
        });

        // Browse
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

                // Don't add duplicates
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (path.toString().equals(model.getValueAt(i, 0))) {
                        return;
                    }
                }

                model.addRow(new Object[]{path.toString()});
                saveIgnoredPaths.run();
            }
        });

        // Save manual edits
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

        content.add(ignoredPathsPanel);

        page.add(content, BorderLayout.NORTH);

        return page;
    }

    private JPanel ffmpegTab() {
        return new FFmpegSettingsTab();
    }
}
