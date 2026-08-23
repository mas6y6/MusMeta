package com.mas6y6.musmeta;

import com.google.gson.JsonObject;
import com.mas6y6.musmeta.config.ConfigBuilder;
import com.mas6y6.musmeta.config.ConfigContainer;
import com.mas6y6.musmeta.config.ConfigManager;
import com.mas6y6.musmeta.config.SubConfig;
import com.mas6y6.musmeta.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigAutoSaveTest {

    @TempDir
    Path tempDir;

    private ConfigManager configManager;
    private Path tempConfigPath;

    @BeforeEach
    public void setUp() {
        configManager = ConfigManager.getInstance();
        configManager.clear();
        ConfigManager.clearCodecs();
        tempConfigPath = tempDir.resolve("config.json");
        configManager.setConfigPath(tempConfigPath);
    }

    @AfterEach
    public void tearDown() {
        configManager.clear();
        ConfigManager.clearCodecs();
        configManager.setConfigPath(null);
    }

    @Test
    public void testContainerSetValueAutoSaves() throws IOException {
        SubConfig appConfig = configManager.registerConfig("app");
        ConfigContainer<String> themeContainer = appConfig.register("theme", "system");

        assertFalse(Files.exists(tempConfigPath));

        themeContainer.setValue("dark");

        assertTrue(Files.exists(tempConfigPath));
        String content = Files.readString(tempConfigPath);
        assertTrue(content.contains("\"theme\": \"dark\""));
    }

    @Test
    public void testSubConfigSetValueAutoSavesRegisteredContainer() throws IOException {
        SubConfig appConfig = configManager.registerConfig("app");
        appConfig.register("volume", 50);

        assertFalse(Files.exists(tempConfigPath));

        appConfig.setValue("volume", 80);

        assertTrue(Files.exists(tempConfigPath));
        String content = Files.readString(tempConfigPath);
        assertTrue(content.contains("\"volume\": 80"));
    }

    @Test
    public void testSubConfigSetValueAutoSavesUnmappedKey() throws IOException {
        SubConfig appConfig = configManager.registerConfig("app");

        assertFalse(Files.exists(tempConfigPath));

        appConfig.setValue("custom_key", "custom_val");

        assertTrue(Files.exists(tempConfigPath));
        String content = Files.readString(tempConfigPath);
        assertTrue(content.contains("\"custom_key\": \"custom_val\""));
    }

    @Test
    public void testResetAutoSaves() throws IOException {
        SubConfig appConfig = configManager.registerConfig("app");
        ConfigContainer<Integer> count = appConfig.register("count", 10);

        count.setValue(99);
        assertTrue(Files.readString(tempConfigPath).contains("\"count\": 99"));

        count.reset();
        assertTrue(Files.readString(tempConfigPath).contains("\"count\": 10"));
    }

    @Test
    public void testResetAllAutoSaves() throws IOException {
        SubConfig appConfig = configManager.registerConfig("app");
        ConfigContainer<String> mode = appConfig.register("mode", "default");
        mode.setValue("custom");
        assertTrue(Files.readString(tempConfigPath).contains("\"mode\": \"custom\""));

        appConfig.resetAll();
        assertTrue(Files.readString(tempConfigPath).contains("\"mode\": \"default\""));
    }

    @Test
    public void testFromClassAutoSaves() throws IOException {
        SubConfig windowConfig = configManager.registerConfig("window");
        ConfigSerializableTest.WindowConfig window = new ConfigSerializableTest.WindowConfig(1920, 1080, true);

        assertFalse(Files.exists(tempConfigPath));

        windowConfig.fromClass(window);

        assertTrue(Files.exists(tempConfigPath));
        String content = Files.readString(tempConfigPath);
        assertTrue(content.contains("\"width\": 1920"));
        assertTrue(content.contains("\"height\": 1080"));
        assertTrue(content.contains("\"fullscreen\": true"));
    }

    @Test
    public void testFromBuilderAutoSaves() throws IOException {
        SubConfig settings = configManager.registerConfig("settings");
        ConfigBuilder builder = new ConfigBuilder();
        builder.setString("language", "en");
        builder.setBoolean("notifications", false);

        assertFalse(Files.exists(tempConfigPath));

        settings.fromBuilder(builder);

        assertTrue(Files.exists(tempConfigPath));
        String content = Files.readString(tempConfigPath);
        assertTrue(content.contains("\"language\": \"en\""));
        assertTrue(content.contains("\"notifications\": false"));
    }

    @Test
    public void testFromJsonAutoSaves() throws IOException {
        SubConfig audio = configManager.registerConfig("audio");
        audio.register("volume", 50);

        JsonObject json = new JsonObject();
        json.addProperty("volume", 75);

        audio.fromJson(json, ConfigManager.GSON);

        assertTrue(Files.exists(tempConfigPath));
        String content = Files.readString(tempConfigPath);
        assertTrue(content.contains("\"volume\": 75"));
    }

    @Test
    public void testLoadDoesNotTriggerAutoSaveDuringLoad() throws IOException {
        String initialJson = """
                {
                  "app": {
                    "auto_ffmpeg_install": true,
                    "updates": "ENABLED"
                  }
                }
                """;
        Files.writeString(tempConfigPath, initialJson);

        Settings.registerConfigs();
        configManager.load();

        SubConfig appConfig = configManager.getConfig("app");
        assertNotNull(appConfig);
        assertEquals(Boolean.TRUE, appConfig.getValue("auto_ffmpeg_install"));

        // Verify content wasn't overwritten prematurely
        String content = Files.readString(tempConfigPath);
        assertTrue(content.contains("\"updates\": \"ENABLED\""));
    }

    @Test
    public void testConfigPersistsAcrossProgramRerun() throws IOException {
        // Run 1: Initial setup
        Settings.registerConfigs();
        SubConfig appConfig = configManager.getConfig("app");
        appConfig.setValue("setup_completed", true);
        appConfig.setValue("ffmpeg_installation_path", "/custom/ffmpeg/path");

        assertTrue(Files.exists(tempConfigPath));

        // Simulate program restart by clearing in-memory ConfigManager
        configManager.clear();
        assertNull(configManager.getConfig("app"));

        // Run 2: Restart behavior matching Main.java
        Settings.registerConfigs();
        if (Files.exists(tempConfigPath)) {
            configManager.load();
        } else {
            configManager.save();
        }

        SubConfig reloadedAppConfig = configManager.getConfig("app");
        assertNotNull(reloadedAppConfig);
        assertEquals(Boolean.TRUE, reloadedAppConfig.getValue("setup_completed"));
        assertEquals("/custom/ffmpeg/path", reloadedAppConfig.getValue("ffmpeg_installation_path"));

        String savedContent = Files.readString(tempConfigPath);
        assertTrue(savedContent.contains("\"setup_completed\": true"));
        assertTrue(savedContent.contains("\"ffmpeg_installation_path\": \"/custom/ffmpeg/path\""));
    }
}
