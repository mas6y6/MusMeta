package com.mas6y6.musmeta.launch;

import com.mas6y6.musmeta.CrashHandler;
import com.mas6y6.musmeta.logging.LogSystem;
import com.mas6y6.musmeta.utils.PlatformSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class Launcher {

    public static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

    private static volatile String[] launchArgs = new String[0];

    private Launcher() {
    }

    public static void main(String[] args) {
        if (args != null) {
            launchArgs = args.clone();
        }
        PlatformSetup.applyMacPlatformSettings();
        LogSystem.init(args);
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler::handle);
        KnotClassLoader knot = new KnotClassLoader(ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(knot);

        try {
            Class<?> bootstrap = Class.forName("com.mas6y6.musmeta.Bootstrap", true, knot);
            bootstrap.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (Throwable t) {
            CrashHandler.handle(Thread.currentThread(), t);
        }
    }

    public static void restart(String[] args) {
        if (args == null) {
            args = launchArgs;
        }
        List<String> command = buildRestartCommand(args);
        LOGGER.info("Restarting MusMeta: {}", command);
        LogSystem.flush();
        try {
            new ProcessBuilder(command).inheritIO().start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to restart MusMeta", e);
        }
        Runtime.getRuntime().exit(0);
    }

    private static List<String> buildRestartCommand(String[] args) {
        List<String> argsList = args == null ? List.of() : Arrays.asList(args);

        Path launcher = findPackagedLauncher();
        if (launcher != null) {
            List<String> command = new ArrayList<>(argsList.size() + 1);
            command.add(launcher.toString());
            command.addAll(argsList);
            return command;
        }

        List<String> command = new ArrayList<>();
        command.add(javaExecutablePath());
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        command.add("-classpath");
        command.add(System.getProperty("java.class.path"));
        command.add(Launcher.class.getName());
        command.addAll(argsList);
        return command;
    }

    private static Path findPackagedLauncher() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path launcher = Path.of(appPath);
            if (Files.isExecutable(launcher)) {
                return launcher;
            }
        }

        Path mainJar = locateMainJar();
        if (mainJar == null) {
            return null;
        }
        Path appDir = mainJar.getParent();
        if (appDir == null || !"app".equals(appDir.getFileName().toString())) {
            return null;
        }
        Path root = appDir.getParent();
        if (root == null) {
            return null;
        }
        if ("Contents".equals(root.getFileName().toString())) {
            Path mac = root.resolve("MacOS/MusMeta");
            if (Files.isExecutable(mac)) {
                return mac;
            }
        }
        Path bin = root.resolve("bin/MusMeta");
        if (Files.isExecutable(bin)) {
            return bin;
        }
        Path exe = root.resolve("MusMeta.exe");
        if (Files.isExecutable(exe)) {
            return exe;
        }
        return null;
    }

    private static Path locateMainJar() {
        try {
            CodeSource codeSource = Launcher.class
                    .getProtectionDomain()
                    .getCodeSource();
            if (codeSource == null) {
                return null;
            }
            URI location = codeSource.getLocation().toURI();
            if (!"file".equals(location.getScheme())) {
                return null;
            }
            Path path = Paths.get(location);
            if (Files.isRegularFile(path)
                    && path.getFileName().toString()
                    .toLowerCase(Locale.ROOT)
                    .endsWith(".jar")) {
                return path;
            }
        } catch (URISyntaxException | SecurityException ignored) {
            return null;
        }
        return null;
    }

    private static String javaExecutablePath() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return Paths.get(
                System.getProperty("java.home"),
                "bin",
                osName.contains("win") ? "java.exe" : "java"
        ).toString();
    }
}