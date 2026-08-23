package com.mas6y6.musmeta;

import com.mas6y6.musmeta.config.ConfigManager;
import com.mas6y6.musmeta.config.SubConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class InstallationCheckTest {

    @TempDir
    Path tempDir;

    private ConfigManager configManager;
    private Path tempConfigPath;

    @BeforeEach
    public void setUp() {
        configManager = ConfigManager.getInstance();
        configManager.clear();
        tempConfigPath = tempDir.resolve("config.json");
        configManager.setConfigPath(tempConfigPath);
    }

    @AfterEach
    public void tearDown() {
        configManager.clear();
        configManager.setConfigPath(null);
    }

    @Test
    public void testFirstInstallationDefaultState() {
        Main.registerConfigs();

        SubConfig appConfig = configManager.getConfig("app");
        assertNotNull(appConfig);

        Boolean isCompleted = appConfig.getValue("setup_completed");
        assertNotNull(isCompleted);
        assertFalse(isCompleted);
    }

    @Test
    public void testInstallationCompletionPersists() throws IOException {
        Main.registerConfigs();

        SubConfig appConfig = configManager.getConfig("app");
        assertNotNull(appConfig);
        appConfig.setValue("setup_completed", true);

        configManager.save();
        assertTrue(Files.exists(tempConfigPath));

        configManager.clear();
        Main.registerConfigs();
        configManager.load();

        SubConfig reloadedConfig = configManager.getConfig("app");
        assertNotNull(reloadedConfig);
        assertEquals(Boolean.TRUE, reloadedConfig.getValue("setup_completed"));
    }

    @Test
    public void testFailsafeResetAndCleanup() throws IOException {
        Main.registerConfigs();

        SubConfig appConfig = configManager.getConfig("app");
        assertNotNull(appConfig);
        appConfig.setValue("setup_completed", true);
        configManager.save();
        assertTrue(Files.exists(tempConfigPath));

        // Perform reset & cleanup
        configManager.resetAll();
        Files.deleteIfExists(configManager.getConfigPath());

        assertFalse(Files.exists(tempConfigPath));
        assertEquals(Boolean.FALSE, appConfig.getValue("setup_completed"));
    }
}
