package com.mas6y6.musmeta.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mas6y6.musmeta.settings.Settings;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FFmpegUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegUtils.class);

    private static final String GITHUB_API =
            "https://api.github.com/repos/GyanD/codexffmpeg/releases/latest";
    private static final String MACOS_URL =
            "https://evermeet.cx/ffmpeg/getrelease/zip";

    /**
     * Returns the Path to the FFmpeg executable.
     * Checks configured settings path first, followed by default app bins and system PATH.
     *
     * @return Path to the FFmpeg executable, or null if not found.
     */
    public static Path getFFmpegExecutable() {
        try {
            if (Settings.FFMPEG_INSTALLATION_PATH != null) {
                String configuredPath = Settings.FFMPEG_INSTALLATION_PATH.get();
                if (configuredPath != null && !configuredPath.isBlank()) {
                    Path path = Paths.get(configuredPath);
                    Path exec = findFFmpegExecutable(path);
                    if (exec != null && Files.isRegularFile(exec)) {
                        return exec;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Path defaultAppBins = Paths.get(System.getProperty("user.home"), ".musmeta", "bins");
            Path exec = findFFmpegExecutable(defaultAppBins);
            if (exec != null && Files.isRegularFile(exec)) {
                return exec;
            }
        } catch (Exception ignored) {
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");

        List<String> candidateNames = isWindows
                ? List.of("ffmpeg.exe", "ffmpeg")
                : List.of("ffmpeg", "ffmpeg.exe");

        for (String candidate : candidateNames) {
            Path sysExec = findSystemExecutable(candidate);
            if (sysExec != null && Files.isRegularFile(sysExec)) {
                return sysExec;
            }
        }

        return null;
    }

    /**
     * Returns the Path to the FFmpeg executable.
     *
     * @return Path to the FFmpeg executable, or null if not found.
     */
    public static Path getFFmpegPath() {
        return getFFmpegExecutable();
    }

    /**
     * Finds and returns the Path to the FFmpeg executable.
     *
     * @return Path to the FFmpeg executable, or null if not found.
     */
    public static Path findFFmpegExecutable() {
        return getFFmpegExecutable();
    }

    /**
     * Finds the FFmpeg executable within the specified bin directory (or directory containing a bin folder).
     */
    public static Path findFFmpegExecutable(Path binDir) {
        if (binDir == null || !Files.exists(binDir)) {
            return null;
        }

        if (Files.isRegularFile(binDir)) {
            String fileName = binDir.getFileName().toString().toLowerCase();
            if (fileName.equals("ffmpeg.exe") || fileName.equals("ffmpeg")) {
                return binDir;
            }
            return null;
        }

        if (!Files.isDirectory(binDir)) {
            return null;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");

        List<String> candidateNames = isWindows
                ? List.of("ffmpeg.exe", "ffmpeg")
                : List.of("ffmpeg", "ffmpeg.exe");

        for (String candidate : candidateNames) {
            Path execPath = binDir.resolve(candidate);
            if (Files.isRegularFile(execPath)) {
                LOGGER.info(String.valueOf(execPath));
                return execPath;
            }
        }

        // Also check if a 'bin' subfolder exists inside the selected directory
        Path subBin = binDir.resolve("bin");
        if (Files.isDirectory(subBin)) {
            for (String candidate : candidateNames) {
                Path execPath = subBin.resolve(candidate);
                if (Files.isRegularFile(execPath)) {
                    LOGGER.info(String.valueOf(execPath));
                    return execPath;
                }
            }
        }

        LOGGER.error("FFmpeg executable not found");
        return null;
    }

    /**
     * Validates that the FFmpeg executable runs successfully and outputs version information.
     */
    public static boolean validateFFmpegExecutable(Path executable) {
        if (executable == null || !Files.isRegularFile(executable)) {
            return false;
        }

        try {
            Process process = new ProcessBuilder(
                    executable.toAbsolutePath().toString(),
                    "-version"
            )
                    .redirectErrorStream(true)
                    .start();

            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            int exitCode = process.waitFor();
            return exitCode == 0 && output.toLowerCase().contains("ffmpeg version");

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /**
     * Validates the FFmpeg bin directory.
     */
    public static boolean validateFFmpegBinDirectory(Path binDir) {
        Path executable = findFFmpegExecutable(binDir);
        if (executable == null) {
            return false;
        }
        return validateFFmpegExecutable(executable);
    }

    public static boolean installFFmpeg(Path installationDir) {
        return installFFmpeg(installationDir, null);
    }

    public static boolean installFFmpeg(Path installationDir, InstallProgressListener listener) {
        try {
            Files.createDirectories(installationDir);

            if (SystemUtils.IS_OS_WINDOWS) {
                return installWindowsFFmpeg(installationDir, listener);
            }

            if (SystemUtils.IS_OS_MAC) {
                return installMacOSFFmpeg(installationDir, listener);
            }

            if (SystemUtils.IS_OS_LINUX) {
                return installLinuxFFmpeg(installationDir, listener);
            }

            throw new UnsupportedOperationException(
                    "FFmpeg installation is not supported on this operating system."
            );

        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onError(e.getMessage() != null ? e.getMessage() : e.toString(), e);
            }
            return false;
        }
    }

    private static boolean installWindowsFFmpeg(
            Path installationDir,
            InstallProgressListener listener
    ) throws IOException, InterruptedException {

        if (listener != null) {
            listener.onProgress("Checking latest FFmpeg release...", -1, "Querying GitHub API");
        }
        LOGGER.info("Getting latest FFmpeg release...");

        GithubRelease release = getLatestFFmpegRelease();

        LOGGER.info(
                "Latest FFmpeg: " + release.tag_name()
        );

        GithubAsset asset = Arrays.stream(release.assets())
                .filter(a ->
                        a.name().endsWith("essentials_build.zip")
                )
                .findFirst()
                .orElseThrow(() ->
                        new IOException(
                                "Could not find FFmpeg essentials ZIP."
                        )
                );

        LOGGER.info(
                "Downloading: " + asset.name()
        );

        Path zip = installationDir.resolve(asset.name());

        downloadFFmpeg(
                asset.browser_download_url(),
                zip,
                listener
        );

        // Verify GitHub's SHA-256 digest if available
        if (asset.digest() != null && !asset.digest().isBlank()) {
            if (listener != null) {
                listener.onProgress("Verifying downloadFFmpeg checksum...", -1, "SHA-256 integrity check");
            }

            if (!verifyDigest(zip, asset.digest())) {
                Files.deleteIfExists(zip);

                throw new IOException(
                        "FFmpeg downloadFFmpeg failed SHA-256 verification."
                );
            }

            LOGGER.info("Download verified.");
        }

        Path extractionDir = installationDir.resolve("ffmpeg");

        deleteDirectory(extractionDir);
        Files.createDirectories(extractionDir);

        if (listener != null) {
            LOGGER.info("Extracting FFmpeg...");
            listener.onProgress("Extracting FFmpeg...", -1, "Unpacking archive");
        }

        extractZip(zip, extractionDir);

        Files.deleteIfExists(zip);

        if (listener != null) {
            LOGGER.info("Validating FFmpeg binary...");
            listener.onProgress("Validating FFmpeg binary...", -1, "Checking executable");
        }

        Path executable = findExecutable(
                extractionDir,
                "ffmpeg.exe"
        );

        if (executable == null) {
            throw new IOException(
                    "Could not find ffmpeg.exe after extraction."
            );
        }

        LOGGER.info(
                "FFmpeg executable: " + executable
        );

        if (!validateFFmpeg(executable)) {
            throw new IOException(
                    "Downloaded FFmpeg failed validation."
            );
        }

        LOGGER.info("FFmpeg installation successful.");
        Settings.FFMPEG_INSTALLATION_PATH.set(String.valueOf(executable));

        if (listener != null) {
            listener.onProgress("FFmpeg installation complete!", 100, "");
        }

        return true;
    }

    private static boolean installMacOSFFmpeg(
            Path installationDir,
            InstallProgressListener listener
    ) throws IOException, InterruptedException {

        if (listener != null) {
            listener.onProgress("Downloading latest macOS FFmpeg...", -1, "Fetching from evermeet.cx");
        }
        LOGGER.info(
                "Downloading latest macOS FFmpeg..."
        );

        Path zip = installationDir.resolve("ffmpeg.zip");

        downloadFFmpeg(
                MACOS_URL,
                zip,
                listener
        );

        Path extractionDir =
                installationDir.resolve("ffmpeg");

        deleteDirectory(extractionDir);
        Files.createDirectories(extractionDir);

        if (listener != null) {
            listener.onProgress("Extracting FFmpeg...", -1, "Unpacking archive");
        }

        extractZip(zip, extractionDir);

        Files.deleteIfExists(zip);

        Path executable = findExecutable(
                extractionDir,
                "ffmpeg"
        );

        if (executable == null) {
            throw new IOException(
                    "Could not find ffmpeg after extraction."
            );
        }

        LOGGER.info(
                "FFmpeg executable: " + executable
        );

        // macOS needs the executable permission.
        try {
            Set<PosixFilePermission> permissions =
                    PosixFilePermissions.fromString("rwxr-xr-x");

            Files.setPosixFilePermissions(
                    executable,
                    permissions
            );
        } catch (UnsupportedOperationException ignored) {
            // Shouldn't happen on normal macOS filesystems,
            // but don't fail solely because POSIX permissions
            // aren't supported.
        }

        if (listener != null) {
            listener.onProgress("Validating FFmpeg binary...", -1, "Checking executable");
        }

        if (!validateFFmpeg(executable)) {
            throw new IOException(
                    "Downloaded FFmpeg failed validation."
            );
        }

        LOGGER.info(
                "FFmpeg installation successful."
        );
        Settings.FFMPEG_INSTALLATION_PATH.set(String.valueOf(executable));

        if (listener != null) {
            listener.onProgress("FFmpeg installation complete!", 100, "");
        }

        return true;
    }

    public static List<LinuxPackageManager> getSupportedLinuxPackageManagers() {
        return List.of(
                new LinuxPackageManager("apt-get", true, List.of(
                        List.of("apt-get", "update"),
                        List.of("apt-get", "install", "-y", "ffmpeg")
                )),
                new LinuxPackageManager("apt", true, List.of(
                        List.of("apt", "update"),
                        List.of("apt", "install", "-y", "ffmpeg")
                )),
                new LinuxPackageManager("dnf", true, List.of(
                        List.of("dnf", "install", "-y", "ffmpeg")
                )),
                new LinuxPackageManager("pacman", true, List.of(
                        List.of("pacman", "-Sy", "--noconfirm", "ffmpeg")
                )),
                new LinuxPackageManager("zypper", true, List.of(
                        List.of("zypper", "--non-interactive", "install", "ffmpeg")
                )),
                new LinuxPackageManager("apk", true, List.of(
                        List.of("apk", "add", "ffmpeg")
                )),
                new LinuxPackageManager("xbps-install", true, List.of(
                        List.of("xbps-install", "-y", "ffmpeg")
                )),
                new LinuxPackageManager("eopkg", true, List.of(
                        List.of("eopkg", "install", "-y", "ffmpeg")
                )),
                new LinuxPackageManager("yum", true, List.of(
                        List.of("yum", "install", "-y", "ffmpeg")
                )),
                new LinuxPackageManager("emerge", true, List.of(
                        List.of("emerge", "media-video/ffmpeg")
                )),
                new LinuxPackageManager("nix-env", false, List.of(
                        List.of("nix-env", "-iA", "nixpkgs.ffmpeg")
                )),
                new LinuxPackageManager("snap", true, List.of(
                        List.of("snap", "install", "ffmpeg")
                )),
                new LinuxPackageManager("flatpak", false, List.of(
                        List.of("flatpak", "install", "-y", "flathub", "org.freedesktop.Platform.ffmpeg-full")
                ))
        );
    }

    public static boolean isCommandAvailable(String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return false;
        }

        String pathEnv = System.getenv("PATH");
        List<String> paths = new ArrayList<>();
        if (pathEnv != null) {
            paths.addAll(Arrays.asList(pathEnv.split(Pattern.quote(File.pathSeparator))));
        }
        paths.addAll(List.of("/usr/bin", "/usr/local/bin", "/bin", "/usr/sbin", "/sbin", "/snap/bin"));

        for (String dir : paths) {
            if (dir == null || dir.isBlank()) continue;
            Path p = Paths.get(dir, commandName);
            if (Files.isRegularFile(p) && Files.isExecutable(p)) {
                return true;
            }
        }

        try {
            Process process = new ProcessBuilder("which", commandName)
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static Path findSystemExecutable(String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return null;
        }

        String pathEnv = System.getenv("PATH");
        List<String> paths = new ArrayList<>();
        if (pathEnv != null) {
            paths.addAll(Arrays.asList(pathEnv.split(Pattern.quote(File.pathSeparator))));
        }
        paths.addAll(List.of("/usr/bin", "/usr/local/bin", "/bin", "/usr/sbin", "/sbin", "/snap/bin"));

        for (String dir : paths) {
            if (dir == null || dir.isBlank()) continue;
            Path p = Paths.get(dir, commandName);
            if (Files.isRegularFile(p) && Files.isExecutable(p)) {
                return p;
            }
        }

        try {
            Process process = new ProcessBuilder("which", commandName)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            if (exitCode == 0 && !output.isEmpty()) {
                Path p = Paths.get(output.split("\\r?\\n")[0].trim());
                if (Files.isRegularFile(p)) {
                    return p;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static boolean installLinuxFFmpeg(
            Path installationDir,
            InstallProgressListener listener
    ) throws IOException, InterruptedException {
        if (listener != null) {
            listener.onProgress("Checking for existing FFmpeg...", -1, "");
        }
        LOGGER.info("Checking for existing FFmpeg on Linux...");

        Path existingExec = findSystemExecutable("ffmpeg");
        if (existingExec != null && validateFFmpeg(existingExec)) {
            LOGGER.info("Found existing system FFmpeg: " + existingExec);
            if (listener != null) {
                listener.onProgress("Existing FFmpeg found on system.", 100, existingExec.toString());
            }
            return setupLinuxInstallationDir(installationDir, existingExec);
        }

        LOGGER.warn("FFmpeg not found. Attempting to install via package manager...");

        List<LinuxPackageManager> packageManagers = getSupportedLinuxPackageManagers();
        LinuxPackageManager selectedManager = null;

        for (LinuxPackageManager pm : packageManagers) {
            if (isCommandAvailable(pm.commandName())) {
                selectedManager = pm;
                break;
            }
        }

        if (selectedManager == null) {
            throw new IOException(
                    "No supported Linux package manager found to install FFmpeg (checked: apt-get, dnf, pacman, zypper, apk, etc.)."
            );
        }

        LOGGER.info("Detected package manager: " + selectedManager.commandName());

        boolean isRoot = Utils.isRunningAsRoot();
        boolean hasPkexec = isCommandAvailable("pkexec");
        boolean hasSudo = isCommandAvailable("sudo");

        for (List<String> commandArgs : selectedManager.installCommands()) {
            List<String> fullCommand = new ArrayList<>();
            if (!isRoot && selectedManager.requiresRoot()) {
                if (hasPkexec) {
                    fullCommand.add("pkexec");
                } else if (hasSudo) {
                    fullCommand.add("sudo");
                }
            }
            fullCommand.addAll(commandArgs);

            String cmdDisplay = String.join(" ", fullCommand);
            if (listener != null) {
                listener.onProgress("Installing FFmpeg via " + selectedManager.commandName() + "...", -1, cmdDisplay);
            }
            LOGGER.info("Executing command: " + cmdDisplay);

            Process process = new ProcessBuilder(fullCommand)
                    .redirectErrorStream(true)
                    .start();

            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            int exitCode = process.waitFor();
            LOGGER.info("Command output:\n" + output);

            if (exitCode != 0) {
                boolean isUpdateCommand = commandArgs.stream().anyMatch(arg -> arg.equalsIgnoreCase("update"));
                if (isUpdateCommand) {
                    LOGGER.warn("Preparation update step failed (non-critical), proceeding with installation...");
                } else {
                    throw new IOException(
                            "Package manager command failed with exit code " + exitCode + ": " + String.join(" ", fullCommand) + "\nOutput: " + output
                    );
                }
            }
        }

        if (listener != null) {
            listener.onProgress("Validating FFmpeg installation...", -1, "");
        }

        Path installedExec = findSystemExecutable("ffmpeg");
        if (installedExec == null) {
            for (String standardPath : List.of("/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg", "/snap/bin/ffmpeg")) {
                Path p = Paths.get(standardPath);
                if (Files.isRegularFile(p)) {
                    installedExec = p;
                    break;
                }
            }
        }

        if (installedExec == null) {
            throw new IOException("FFmpeg was not found on the system after package manager installation.");
        }

        if (!validateFFmpeg(installedExec)) {
            throw new IOException("Installed FFmpeg failed validation: " + installedExec);
        }

        LOGGER.info("FFmpeg installation successful.");
        if (listener != null) {
            listener.onProgress("FFmpeg installation complete!", 100, "");
        }
        return setupLinuxInstallationDir(installationDir, installedExec);
    }

    private static boolean setupLinuxInstallationDir(Path installationDir, Path systemFfmpeg) throws IOException {
        Path extractionDir = installationDir.resolve("ffmpeg");
        deleteDirectory(extractionDir);
        Files.createDirectories(extractionDir);

        Path linkOrCopy = extractionDir.resolve("ffmpeg");
        try {
            Files.createSymbolicLink(linkOrCopy, systemFfmpeg.toAbsolutePath());
        } catch (Exception e) {
            try {
                Files.copy(systemFfmpeg, linkOrCopy, StandardCopyOption.REPLACE_EXISTING);
                try {
                    Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-xr-x");
                    Files.setPosixFilePermissions(linkOrCopy, permissions);
                } catch (Exception ignored) {
                }
            } catch (Exception copyEx) {
                LOGGER.error("Could not create symlink or copy to " + linkOrCopy + ": " + copyEx.getMessage());
            }
        }

        return true;
    }

    private static Path downloadFFmpeg(
            String url,
            Path destination,
            InstallProgressListener listener
    ) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(
                        HttpClient.Redirect.NORMAL
                )
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        if (listener != null) {
            listener.onProgress("Connecting to downloadFFmpeg server...", -1, "");
        }

        HttpResponse<InputStream> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() != 200) {
            response.body().close();

            throw new IOException(
                    "Download failed with HTTP "
                            + response.statusCode()
            );
        }

        long contentLength = response.headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1L);

        if (destination.getParent() != null) {
            Files.createDirectories(
                    destination.getParent()
            );
        }

        byte[] buffer = new byte[8192];
        long totalBytesRead = 0;
        long lastUpdate = 0;

        try (
                InputStream input = response.body();
                OutputStream output = Files.newOutputStream(
                        destination,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                )
        ) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                totalBytesRead += read;

                long now = System.currentTimeMillis();
                if (listener != null && (now - lastUpdate >= 50 || (contentLength > 0 && totalBytesRead == contentLength))) {
                    lastUpdate = now;
                    int percent = contentLength > 0 ? (int) ((totalBytesRead * 100) / contentLength) : -1;
                    String detail;
                    if (contentLength > 0) {
                        detail = String.format(Locale.US, "%.1f MB / %.1f MB (%d%%)",
                                totalBytesRead / (1024.0 * 1024.0),
                                contentLength / (1024.0 * 1024.0),
                                percent);
                    } else {
                        detail = String.format(Locale.US, "%.1f MB downloaded", totalBytesRead / (1024.0 * 1024.0));
                    }
                    LOGGER.info("Downloading FFmpeg...", percent, detail);
                    listener.onProgress("Downloading FFmpeg...", percent, detail);
                }
            }
        }

        return destination;
    }

    public static String getSingleRootDirectoryPrefix(Path zip) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            String rootDir = null;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');

                while (name.startsWith("/")) {
                    name = name.substring(1);
                }

                if (name.isEmpty() || name.equals("./")) {
                    continue;
                }

                int firstSlash = name.indexOf('/');
                if (firstSlash == -1) {
                    if (entry.isDirectory()) {
                        String dirName = name;
                        if (dirName.equals("..") || dirName.equals(".")) {
                            return null;
                        }
                        if (rootDir == null) {
                            rootDir = dirName;
                        } else if (!rootDir.equals(dirName)) {
                            return null;
                        }
                    } else {
                        // Regular file at root level -> not wrapped in a single root folder
                        return null;
                    }
                } else {
                    String topDir = name.substring(0, firstSlash);
                    if (topDir.equals("..") || topDir.equals(".")) {
                        return null;
                    }
                    if (rootDir == null) {
                        rootDir = topDir;
                    } else if (!rootDir.equals(topDir)) {
                        return null;
                    }
                }
            }

            return rootDir != null ? rootDir + "/" : null;
        }
    }

    public static void extractZip(
            Path zip,
            Path destination
    ) throws IOException {
        String rootPrefix = getSingleRootDirectoryPrefix(zip);
        Path normalizedDest = destination.toAbsolutePath().normalize();

        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');

                while (name.startsWith("/")) {
                    name = name.substring(1);
                }

                if (rootPrefix != null && name.startsWith(rootPrefix)) {
                    name = name.substring(rootPrefix.length());
                }

                if (name.isEmpty() || name.equals("/")) {
                    continue;
                }

                Path output = normalizedDest.resolve(name).normalize();

                // Prevent ZIP path traversal.
                if (!output.startsWith(normalizedDest)) {
                    throw new IOException(
                            "Unsafe ZIP entry: " + entry.getName()
                    );
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    continue;
                }

                if (output.getParent() != null) {
                    Files.createDirectories(output.getParent());
                }

                try (InputStream in = zipFile.getInputStream(entry);
                     OutputStream out = Files.newOutputStream(
                             output,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING
                     )) {
                    in.transferTo(out);
                }
            }
        }
    }

    private static Path findExecutable(
            Path directory,
            String filename
    ) throws IOException {

        try (var stream = Files.walk(directory)) {

            return stream
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .equalsIgnoreCase(filename)
                    )
                    .findFirst()
                    .orElse(null);
        }
    }

    private static boolean validateFFmpeg(
            Path executable
    ) throws IOException, InterruptedException {

        if (!Files.isRegularFile(executable)) {
            return false;
        }

        Process process = new ProcessBuilder(
                executable.toString(),
                "-version"
        )
                .redirectErrorStream(true)
                .start();

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        int exitCode = process.waitFor();

        String version = output.lines()
                .findFirst()
                .orElse("Unknown version");

        LOGGER.info(
                "Detected: " + version
        );

        return exitCode == 0
                && output.toLowerCase(Locale.ROOT)
                .contains("ffmpeg version");
    }

    private static boolean verifyDigest(
            Path file,
            String expectedDigest
    ) throws IOException {

        if (expectedDigest == null || expectedDigest.isBlank()) {
            return false;
        }

        String expected = expectedDigest.startsWith("sha256:")
                ? expectedDigest.substring("sha256:".length())
                : expectedDigest;

        try (InputStream input =
                     Files.newInputStream(file)) {

            var digest = java.security.MessageDigest
                    .getInstance("SHA-256");

            byte[] buffer = new byte[8192];

            int read;

            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

            String actual = HexFormat.of()
                    .formatHex(digest.digest());

            return actual.equalsIgnoreCase(expected);

        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(
                    "SHA-256 is not available.",
                    e
            );
        }
    }

    private static void deleteDirectory(
            Path directory
    ) throws IOException {

        if (!Files.exists(directory)) {
            return;
        }

        try (var stream = Files.walk(directory)) {

            stream.sorted(
                    Comparator.reverseOrder()
            ).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static GithubRelease getLatestFFmpegRelease()
            throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API))
                .header(
                        "Accept",
                        "application/vnd.github+json"
                )
                .header("User-Agent", "MusMeta")
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "GitHub returned HTTP "
                            + response.statusCode()
            );
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper.readValue(
                response.body(),
                GithubRelease.class
        );
    }

    public interface InstallProgressListener {
        void onProgress(String status, int percentage, String details);

        default void onError(String errorMessage, Throwable throwable) {
        }
    }

    public record LinuxPackageManager(
            String commandName,
            boolean requiresRoot,
            List<List<String>> installCommands
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GithubAsset(
            String name,
            String browser_download_url,
            long size,
            String digest
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GithubRelease(
            String tag_name,
            String name,
            GithubAsset[] assets
    ) {}
}
