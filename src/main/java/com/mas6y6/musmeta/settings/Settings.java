package com.mas6y6.musmeta.settings;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.mas6y6.musmeta.config.ConfigContainer;
import com.mas6y6.musmeta.config.ConfigManager;

import javax.swing.*;

public class Settings {
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
            ConfigManager.getInstance().getConfig("app").register("setup_completed", Updates.ENABLED);

    public static ConfigContainer<Theme> PREFERRED_THEME =
            ConfigManager.getInstance().getConfig("app").register("preferred_theme", Theme.SYSTEM);

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
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
