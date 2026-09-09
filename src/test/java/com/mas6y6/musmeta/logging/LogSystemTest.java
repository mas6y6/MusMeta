package com.mas6y6.musmeta.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogSystemTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogSystemTest.class);

    @AfterEach
    void tearDown() {
        LogSystem.shutdown();
        LogSystem.setCustomLogsDirectory(null);
    }

    @Test
    void testLogInitializationAndWriting(@TempDir Path tempDir) throws IOException {
        LogSystem.setCustomLogsDirectory(tempDir);
        LogSystem.init();

        assertTrue(LogSystem.isInitialized());
        Path latestLog = LogSystem.getLatestLogPath();
        assertTrue(Files.exists(latestLog));

        System.out.println("Standard out test message 12345");
        System.err.println("Standard err test message 67890");
        LOGGER.info("SLF4J test message ABCDE");

        LogSystem.flush();

        String content = Files.readString(latestLog);
        assertTrue(content.contains("Standard out test message 12345"), "Should contain stdout message");
        assertTrue(content.contains("Standard err test message 67890"), "Should contain stderr message");
        assertTrue(content.contains("SLF4J test message ABCDE"), "Should contain SLF4J log message");
    }

    @Test
    void testLogRotation(@TempDir Path tempDir) throws IOException {
        LogSystem.setCustomLogsDirectory(tempDir);
        LogSystem.init();

        System.out.println("First session message");
        LogSystem.flush();
        LogSystem.shutdown();

        Path latestLog = tempDir.resolve("latest.log");
        assertTrue(Files.exists(latestLog));
        assertTrue(Files.readString(latestLog).contains("First session message"));

        // Second session should rotate first latest.log to date-based log
        LogSystem.init();
        System.out.println("Second session message");
        LogSystem.flush();
        LogSystem.shutdown();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path archived1 = tempDir.resolve(today + "-1.log");
        assertTrue(Files.exists(archived1), "Archived log 1 should exist");
        assertTrue(Files.readString(archived1).contains("First session message"));

        Path newLatest = tempDir.resolve("latest.log");
        assertTrue(Files.exists(newLatest));
        assertTrue(Files.readString(newLatest).contains("Second session message"));

        // Third session should rotate to -2.log
        LogSystem.init();
        System.out.println("Third session message");
        LogSystem.flush();
        LogSystem.shutdown();

        Path archived2 = tempDir.resolve(today + "-2.log");
        assertTrue(Files.exists(archived2), "Archived log 2 should exist");
        assertTrue(Files.readString(archived2).contains("Second session message"));
    }

    @Test
    void testCustomAndDefaultDirectories(@TempDir Path tempDir) {
        LogSystem.setCustomLogsDirectory(tempDir);
        assertEquals(tempDir, LogSystem.getLogsDirectory());
        assertEquals(tempDir.resolve("latest.log"), LogSystem.getLatestLogPath());

        LogSystem.setCustomLogsDirectory(null);
        // Under test environment, default logs directory should not point to user home ~/.musmeta/logs
        Path userHomeLogs = Path.of(System.getProperty("user.home"), ".musmeta", "logs");
        org.junit.jupiter.api.Assertions.assertNotEquals(userHomeLogs, LogSystem.getLogsDirectory());
    }
}
