package com.mas6y6.musmeta.ui.subwindows.settings;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.jthemedetecor.OsThemeDetector;
import com.mas6y6.musmeta.Main;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.settings.Theme;
import com.mas6y6.musmeta.ui.components.ColorPickerField;
import com.mas6y6.musmeta.ui.subwindows.settings.base.SettingTab;
import com.mas6y6.musmeta.utils.ColorWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class AppearanceTab extends SettingTab {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppearanceTab.class);

    private final JRadioButton systemDefaultBtn =
            new JRadioButton("Automatic with system");
    private final JRadioButton lightBtn =
            new JRadioButton("Light");
    private final JRadioButton darkBtn =
            new JRadioButton("Dark");

    public AppearanceTab() {
        super();

        JLabel title = new JLabel("Appearance");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        CONTENT.add(title);

        CONTENT.add(Box.createVerticalStrut(20));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(systemDefaultBtn);
        buttonGroup.add(lightBtn);
        buttonGroup.add(darkBtn);

        systemDefaultBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        CONTENT.add(systemDefaultBtn);
        CONTENT.add(Box.createVerticalStrut(8));

        lightBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        CONTENT.add(lightBtn);
        CONTENT.add(Box.createVerticalStrut(8));

        darkBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        CONTENT.add(darkBtn);
        CONTENT.add(Box.createVerticalStrut(8));
        CONTENT.add(new JSeparator());
        CONTENT.add(Box.createVerticalStrut(8));
        CONTENT.add(new JLabel("Accent:"));
        CONTENT.add(Box.createVerticalStrut(8));

        ColorPickerField colorPickerField = new ColorPickerField(Settings.ACCENT.get());
        colorPickerField.setAlignmentX(Component.LEFT_ALIGNMENT);
        colorPickerField.addListener(color -> {
            Settings.ACCENT.set(ColorWrapper.addWrapper(color).toHex());
            Main.applyTheme();
        });
        CONTENT.add(colorPickerField);

        systemDefaultBtn.addActionListener(
                e -> {
                    Settings.PREFERRED_THEME.set(Theme.SYSTEM);
                    try {
                        FlatLaf.setGlobalExtraDefaults( Collections.singletonMap( "@accentColor", Settings.ACCENT.get() ) );

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
                e -> Settings.PREFERRED_THEME.set(Theme.LIGHT)
        );

        darkBtn.addActionListener(
                e -> Settings.PREFERRED_THEME.set(Theme.DARK)
        );



        configureInitialState();
    }

    private void configureInitialState() {
        if (Settings.PREFERRED_THEME.get() == Theme.SYSTEM) {
            systemDefaultBtn.setSelected(true);
        } else if (Settings.PREFERRED_THEME.get() == Theme.LIGHT) {
            lightBtn.setSelected(true);
        } else {
            darkBtn.setSelected(true);
        }
    }
}