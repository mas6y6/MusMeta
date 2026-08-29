package com.mas6y6.musmeta;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.util.SystemInfo;
import com.jthemedetecor.OsThemeDetector;
import com.mas6y6.musmeta.config.ConfigManager;
import com.mas6y6.musmeta.config.SubConfig;
import com.mas6y6.musmeta.settings.ConfigCodecs;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.settings.Theme;
import com.mas6y6.musmeta.settings.Updates;
import com.mas6y6.musmeta.ui.MainWindow;
import com.mas6y6.musmeta.ui.prompts.PostInstallationPrompt;
import org.slf4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static final Path appDir = Paths.get(System.getProperty("user.home"), ".musmeta");
    public static Font outfitMedium;
    public static Font outfitExtraBold;

    public static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Main.class);

    /* Updated this to be Public Static main because
     * most JDKs will have issues running the project if Main is declared as
     * a private package. Changed tit to Public Static void for compatability. sake. - Batista */
    public static void main() {
        LOGGER.info("com.mas6y6.musmeta.Main.main()");

        if (SystemInfo.isMacOS) {
            System.setProperty( "apple.awt.application.name", "MusMeta" );
        }

        if (!Files.exists(appDir)) {
            try {
                Files.createDirectories(appDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create .musmeta directory", e);
            }
        }

        ConfigManager configManager = ConfigManager.getInstance();
        ConfigCodecs.register();

        Path configPath = configManager.getConfigPath();

        Settings.registerConfigs();

        if (Files.exists(configPath)) {
            try {
                configManager.load();
            } catch (IOException e) {
                throw new RuntimeException("Failed to load config: " + e.getMessage());
            }
        } else {
            try {
                configManager.save();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create config: " + e.getMessage(), e);
            }
        }

        /* Tray catch block for applying Outfit-VariableFont_wght globally.
        * - Batista 8/22/2026  */
        try {
            InputStream isLight = Main.class.getResourceAsStream("/font/Outfit-Light.ttf");
            InputStream isMedium = Main.class.getResourceAsStream("/font/Outfit-Medium.ttf");
            InputStream isExtraBold = Main.class.getResourceAsStream("/font/Outfit-ExtraBold.ttf");
            if (isMedium != null && isExtraBold != null && isLight != null) {
                Font outfitLight = Font.createFont(Font.TRUETYPE_FONT, isLight);
                Font outfitMedium = Font.createFont(Font.TRUETYPE_FONT, isMedium);
                Font outfitExtraBold = Font.createFont(Font.TRUETYPE_FONT, isExtraBold);

                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(outfitLight);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(outfitMedium);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(outfitExtraBold);

                // Registered with FontUIResource -Batista
                UIManager.put("defaultFont", new javax.swing.plaf.FontUIResource(outfitMedium.deriveFont(14f)));
            } else {
                LOGGER.error("Outfit static font resources not found.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Outfit fonts", e);
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
            SwingUtilities.invokeLater(() -> MainWindow.INSTANCE.setVisible(true));
        }
    }
}
