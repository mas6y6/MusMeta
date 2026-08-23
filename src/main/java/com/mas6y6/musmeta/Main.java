package com.mas6y6.musmeta;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.jthemedetecor.OsThemeDetector;
import com.mas6y6.musmeta.config.ConfigManager;
import com.mas6y6.musmeta.config.SubConfig;
import com.mas6y6.musmeta.settings.Theme;
import com.mas6y6.musmeta.settings.Updates;
import com.mas6y6.musmeta.ui.prompts.PostInstallationPrompt;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static final Path appDir = Paths.get(System.getProperty("user.home"), ".musmeta");

    static void main(String[] args) throws IOException {
        System.out.println("com.mas6y6.musmeta.Main.main()");

        if (!Files.exists(appDir)) {
            try {
                Files.createDirectories(appDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create .musmeta directory", e);
            }
        }

        ConfigManager configManager = ConfigManager.getInstance();
        Path configPath = configManager.getConfigPath();

        registerConfigs();
        configManager.save();

        if (Files.exists(configPath)) {
            try {
                configManager.load();
            } catch (IOException e) {
                System.err.println("Failed to load config: " + e.getMessage());
            }
        }

        JFrame.setDefaultLookAndFeelDecorated(true);

        if (OsThemeDetector.isSupported()) {
            if (OsThemeDetector.getDetector().isDark()) {
                try {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            OsThemeDetector.getDetector().registerListener(isDark -> {
                try {
                    if (configManager.getConfig("app").getValue("preferred_theme") == Theme.DARK) {
                        if (isDark) {
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
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            FlatLightLaf.setup();
        }

        SubConfig appConfig = configManager.getConfig("app");
        boolean isSetupCompleted = appConfig != null && Boolean.TRUE.equals(appConfig.getValue("setup_completed"));

        if (!isSetupCompleted) {
            SwingUtilities.invokeLater(PostInstallationPrompt::new);
        } else {
            SwingUtilities.invokeLater(() -> {
                // Main application window launch
            });
        }
    }

    public static void registerConfigs() {
        var manager = ConfigManager.getInstance();
        var appConfig = manager.registerConfig("app");
        appConfig.register("setup_completed", false);
        appConfig.register("auto_ffmpeg_install", true);
        appConfig.register("ffmpeg_installation_path", "");
        appConfig.register("updates", Updates.ENABLED);
        appConfig.register("preferred_theme", Theme.SYSTEM);

        ConfigManager.getInstance().getConfig("app").getContainer("preferred_theme").addListener((value) -> {
            try {
                if (value == Theme.DARK) {
                    FlatAnimatedLafChange.showSnapshot();
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    FlatLaf.updateUI();
                    FlatAnimatedLafChange.hideSnapshotWithAnimation();
                } else if (value == Theme.LIGHT) {
                    FlatAnimatedLafChange.showSnapshot();
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    FlatLaf.updateUI();
                    FlatAnimatedLafChange.hideSnapshotWithAnimation();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
