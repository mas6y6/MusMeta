package com.mas6y6.musmeta.settings;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.google.gson.reflect.TypeToken;
import com.jthemedetecor.OsThemeDetector;
import com.mas6y6.musmeta.config.ConfigContainer;
import com.mas6y6.musmeta.config.ConfigManager;
import com.mas6y6.musmeta.core.Core;
import org.slf4j.Logger;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Settings {
    static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Settings.class);

    static {
        var manager = ConfigManager.getInstance();
        var appConfig = manager.registerConfig("app");
    }

    public static ConfigContainer<Boolean> SETUP_COMPLETED =
            ConfigManager.getInstance().getConfig("app").register("setup_completed", false);

    public static ConfigContainer<Boolean> AUTO_FFMPEG_INSTALL =
            ConfigManager.getInstance().getConfig("app").register("auto_ffmpeg_install", true);

    public static ConfigContainer<String> FFMPEG_INSTALLATION_PATH =
            ConfigManager.getInstance().getConfig("app").register("ffmpeg_installation_path", "");

    public static ConfigContainer<Updates> UPDATES =
            ConfigManager.getInstance().getConfig("app").register("updates", Updates.ENABLED);

    public static ConfigContainer<Theme> PREFERRED_THEME =
            ConfigManager.getInstance().getConfig("app").register("preferred_theme", Theme.SYSTEM);

    public static ConfigContainer<Path> MUSIC_DIRECTORY_PATH =
            ConfigManager.getInstance().getConfig("app").register("music_directory_path", Paths.get(System.getProperty("user.home"),"music"));

    public static ConfigContainer<List<Path>> MUSIC_SCAN_IGNORE_PATHS =
            ConfigManager.getInstance().getConfig("app").register("music_scan_ignore_paths", List.of(),
                    new TypeToken<List<Path>>() {});

    private Settings() {}

    public static void registerConfigs() {
        PREFERRED_THEME.addListener((value) -> {
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
                } else {
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
                }
            } catch (Exception e) {
                LOGGER.error("Error setting theme", e);
            }
        });
    }
}
