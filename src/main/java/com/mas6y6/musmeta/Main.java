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
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class Main {

    public static final Path appDir = Paths.get(System.getProperty("user.home"), ".musmeta");
    public static Font outfitMedium;
    public static Font outfitExtraBold;

    /* Updated this to be Public Static main because
     * most JDKs will have issues running the project if Main is declared as
     * a private package. Changed tit to Public Static void for compatability. sake. - Batista */
    public static void main(String[] args) throws IOException {
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

        /* Tray catch block for applying Outfit-VariableFont_wght globally.
        * - Batista 8/22/2026  */
        try {
            InputStream isMedium = Main.class.getResourceAsStream("/font/Outfit-Medium.ttf");
            InputStream isExtraBold = Main.class.getResourceAsStream("/font/Outfit-ExtraBold.ttf");
            if (isMedium != null && isExtraBold != null) {
                outfitMedium = Font.createFont(Font.TRUETYPE_FONT, isMedium);
                outfitExtraBold = Font.createFont(Font.TRUETYPE_FONT, isExtraBold);
                
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(outfitMedium);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(outfitExtraBold);

                // Registered with FontUIResource -Batista
                UIManager.put("defaultFont", new javax.swing.plaf.FontUIResource(outfitMedium.deriveFont(14f)));
            } else {
                System.err.println("Outfit static font resources not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
