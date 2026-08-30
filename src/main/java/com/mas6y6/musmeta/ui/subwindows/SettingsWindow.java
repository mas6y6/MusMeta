package com.mas6y6.musmeta.ui.subwindows;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.jthemedetecor.OsThemeDetector;
import com.mas6y6.musmeta.registry.Registries;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.settings.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

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

        JLabel title = new JLabel("Appearance");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        page.add(content, BorderLayout.NORTH);

        return page;
    }
}
