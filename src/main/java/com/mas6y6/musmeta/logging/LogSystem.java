package com.mas6y6.musmeta.logging;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LogSystem {

    private static final String LATEST_LOG_FILENAME = "latest.log";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Object FILE_LOCK = new Object();
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private static Path customLogsDirectory;
    private static OutputStream fileOutputStream;
    private static PrintStream originalOut;
    private static PrintStream originalErr;
    private static PrintStream teeOut;
    private static PrintStream teeErr;

    private LogSystem() {
    }

    public static Path defaultLogsDirectory() {
        String configured = System.getProperty("musmeta.logs.dir");
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("musmeta.logs");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        String home = System.getProperty("musmeta.home");
        if (home != null && !home.isBlank()) {
            return Path.of(home, "logs");
        }
        if (com.mas6y6.musmeta.config.ConfigManager.isTestEnvironment()) {
            String testDir = System.getProperty("musmeta.test.dir");
            if (testDir != null && !testDir.isBlank()) {
                return Path.of(testDir, "logs");
            }
            return Paths.get(System.getProperty("java.io.tmpdir"), "musmeta-test", "logs");
        }
        return Paths.get(System.getProperty("user.home"), ".musmeta", "logs");
    }

    public static Path getLogsDirectory() {
        return customLogsDirectory != null ? customLogsDirectory : defaultLogsDirectory();
    }

    public static void setCustomLogsDirectory(Path path) {
        customLogsDirectory = path;
    }

    public static Path getLatestLogPath() {
        return getLogsDirectory().resolve(LATEST_LOG_FILENAME);
    }

    public static boolean isInitialized() {
        return INITIALIZED.get();
    }

    public static synchronized void init() {
        init(new String[0]);
    }

    public static synchronized void init(String[] args) {
        if (INITIALIZED.get()) {
            return;
        }

        boolean debug = isDebugRequested(args);
        configureSlf4jProperties(debug);

        Path logsDir = getLogsDirectory();
        try {
            Files.createDirectories(logsDir);

            Path latestLog = logsDir.resolve(LATEST_LOG_FILENAME);
            if (Files.exists(latestLog) && Files.size(latestLog) > 0) {
                rotateLog(latestLog, logsDir);
            }

            fileOutputStream = new BufferedOutputStream(
                    Files.newOutputStream(
                            latestLog,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    )
            );

            originalOut = System.out;
            originalErr = System.err;

            teeOut = new PrintStream(new TeeOutputStream(originalOut, fileOutputStream, FILE_LOCK), true, StandardCharsets.UTF_8);
            teeErr = new PrintStream(new TeeOutputStream(originalErr, fileOutputStream, FILE_LOCK), true, StandardCharsets.UTF_8);

            System.setOut(teeOut);
            System.setErr(teeErr);

            INITIALIZED.set(true);

            Runtime.getRuntime().addShutdownHook(new Thread(LogSystem::shutdown, "MusMeta-LogSystem-Shutdown"));

        } catch (IOException e) {
            System.err.println("Warning: Failed to initialize MusMeta logging to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isDebugRequested(String[] args) {
        if (Boolean.getBoolean("musmeta.debug")) {
            return true;
        }
        if (args != null) {
            for (String arg : args) {
                if ("--debug".equalsIgnoreCase(arg) || "-d".equalsIgnoreCase(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void configureSlf4jProperties(boolean debug) {
        setPropertyIfAbsent("org.slf4j.simpleLogger.showDateTime", "true");
        setPropertyIfAbsent("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        setPropertyIfAbsent("org.slf4j.simpleLogger.showThreadName", "true");
        setPropertyIfAbsent("org.slf4j.simpleLogger.showLogName", "true");
        setPropertyIfAbsent("org.slf4j.simpleLogger.showShortLogName", "true");

        if (debug) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        } else {
            setPropertyIfAbsent("org.slf4j.simpleLogger.defaultLogLevel", "info");
        }
    }

    private static void setPropertyIfAbsent(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }

    private static void rotateLog(Path latestLog, Path logsDir) {
        try {
            LocalDate date;
            try {
                FileTime lastModified = Files.getLastModifiedTime(latestLog);
                date = lastModified.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception e) {
                date = LocalDate.now();
            }

            String datePrefix = date.format(DATE_FORMATTER);
            int index = 1;
            Path archivedPath;
            do {
                archivedPath = logsDir.resolve(datePrefix + "-" + index + ".log");
                index++;
            } while (Files.exists(archivedPath));

            Files.move(latestLog, archivedPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Warning: Failed to rotate previous log file: " + e.getMessage());
        }
    }

    public static synchronized void flush() {
        if (fileOutputStream != null) {
            synchronized (FILE_LOCK) {
                try {
                    fileOutputStream.flush();
                } catch (IOException ignored) {
                }
            }
        }
        if (teeOut != null) {
            teeOut.flush();
        }
        if (teeErr != null) {
            teeErr.flush();
        }
    }

    public static synchronized void shutdown() {
        if (!INITIALIZED.get()) {
            return;
        }

        flush();

        if (fileOutputStream != null) {
            synchronized (FILE_LOCK) {
                try {
                    fileOutputStream.close();
                } catch (IOException ignored) {
                }
                fileOutputStream = null;
            }
        }

        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }

        INITIALIZED.set(false);
    }

    private static class TeeOutputStream extends OutputStream {
        private final OutputStream console;
        private final OutputStream file;
        private final Object lock;

        public TeeOutputStream(OutputStream console, OutputStream file, Object lock) {
            this.console = Objects.requireNonNull(console, "console cannot be null");
            this.file = Objects.requireNonNull(file, "file cannot be null");
            this.lock = Objects.requireNonNull(lock, "lock cannot be null");
        }

        @Override
        public void write(int b) throws IOException {
            console.write(b);
            synchronized (lock) {
                file.write(b);
                if (b == '\n') {
                    file.flush();
                }
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            console.write(b, off, len);
            synchronized (lock) {
                file.write(b, off, len);
                for (int i = off; i < off + len; i++) {
                    if (b[i] == '\n') {
                        file.flush();
                        break;
                    }
                }
            }
        }

        @Override
        public void flush() throws IOException {
            console.flush();
            synchronized (lock) {
                file.flush();
            }
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
