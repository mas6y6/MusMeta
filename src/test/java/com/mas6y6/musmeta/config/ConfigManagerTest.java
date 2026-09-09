package com.mas6y6.musmeta.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @TempDir
    Path tempDir;

    private String originalMusmetaConfig;
    private String originalMusmetaConfigPath;
    private String originalMusmetaHome;

    @BeforeEach
    void setUp() {
        originalMusmetaConfig = System.getProperty("musmeta.config");
        originalMusmetaConfigPath = System.getProperty("musmeta.config.path");
        originalMusmetaHome = System.getProperty("musmeta.home");
        ConfigManager.resetInstance();
    }

    @AfterEach
    void tearDown() {
        restoreProperty("musmeta.config", originalMusmetaConfig);
        restoreProperty("musmeta.config.path", originalMusmetaConfigPath);
        restoreProperty("musmeta.home", originalMusmetaHome);
        ConfigManager.resetInstance();
    }

    private void restoreProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.clearProperty(key);
        }
    }

    @Test
    void testTestEnvironmentDetectionDoesNotUseUserHome() {
        System.clearProperty("musmeta.config");
        System.clearProperty("musmeta.config.path");
        System.clearProperty("musmeta.home");

        assertTrue(ConfigManager.isTestEnvironment(), "Should detect running within JUnit test environment");

        Path defaultPath = ConfigManager.getDefaultConfigPath();
        Path userHomeConfig = Paths.get(System.getProperty("user.home"), ".musmeta", "config.json");

        assertNotEquals(userHomeConfig, defaultPath, "Default config in test environment must not point to ~/.musmeta/config.json");
    }

    @Test
    void testSystemPropertyOverrides() {
        Path customPath = tempDir.resolve("custom-config.json");

        System.setProperty("musmeta.config", customPath.toString());
        assertEquals(customPath, ConfigManager.getDefaultConfigPath());

        Path overridePath = tempDir.resolve("override-config.json");
        System.setProperty("musmeta.config.path", overridePath.toString());
        assertEquals(overridePath, ConfigManager.getDefaultConfigPath());

        System.clearProperty("musmeta.config.path");
        System.clearProperty("musmeta.config");

        Path homeDir = tempDir.resolve("home");
        System.setProperty("musmeta.home", homeDir.toString());
        assertEquals(homeDir.resolve("config.json"), ConfigManager.getDefaultConfigPath());
    }

    @Test
    void testCustomConfigPathOnInstance() throws IOException {
        ConfigManager manager = ConfigManager.getInstance();
        Path customPath = tempDir.resolve("isolated.json");
        manager.setCustomConfigPath(customPath);

        assertEquals(customPath, manager.getConfigPath());

        SubConfig app = manager.registerConfig("app");
        ConfigContainer<String> nameProp = app.register("app_name", "TestApp");
        nameProp.set("MusMetaIsolated");

        manager.save();

        assertTrue(Files.exists(customPath), "Config file should be saved to custom path");
        String content = Files.readString(customPath);
        assertTrue(content.contains("MusMetaIsolated"));

        // Ensure user home config was NOT touched
        Path userHomeConfig = Paths.get(System.getProperty("user.home"), ".musmeta", "config.json");
        // User home should not match customPath
        assertNotEquals(userHomeConfig, customPath);
    }

    @Test
    void testSaveAndLoadIsolation() throws IOException {
        Path customPath = tempDir.resolve("nested").resolve("config.json");
        ConfigManager manager = ConfigManager.getInstance();
        manager.setConfigPath(customPath);

        SubConfig testConfig = manager.registerConfig("test");
        ConfigContainer<Integer> numberProp = testConfig.register("count", 10);
        numberProp.set(42);

        assertTrue(Files.exists(customPath));

        ConfigManager newManager = ConfigManager.getInstance();
        newManager.clear();
        newManager.setConfigPath(customPath);
        SubConfig reloadedConfig = newManager.registerConfig("test");
        ConfigContainer<Integer> reloadedProp = reloadedConfig.register("count", 0);

        newManager.load();
        assertEquals(42, reloadedProp.get());
    }
}
