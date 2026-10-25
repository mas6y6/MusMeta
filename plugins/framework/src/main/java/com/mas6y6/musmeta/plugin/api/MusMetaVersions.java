package com.mas6y6.musmeta.plugin.api;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MusMetaVersions {
    public static final String ANY = "*";

    private static final String RESOURCE = "/musmeta.properties";
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private static final @Nullable String RUNNING_VERSION = loadRunningVersion();

    private MusMetaVersions() {
    }

    public static @Nullable String runningVersion() {
        return RUNNING_VERSION;
    }

    private static @Nullable String loadRunningVersion() {
        Properties properties = new Properties();
        try (InputStream input = MusMetaVersions.class.getResourceAsStream(RESOURCE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
        }

        String declared = properties.getProperty("musmetaVersion", properties.getProperty("version", ""));
        if (declared.isBlank()) {
            return null;
        }

        String version = declared.trim();
        return parse(version) == null ? null : version;
    }

    public static boolean isCompatible(@Nullable String required) {
        if (required == null || required.isBlank() || required.trim().equals(ANY)) {
            return true;
        }

        Minor requiredVersion = parse(required);
        if (requiredVersion == null) {
            return true;
        }

        Minor run = parse(runningVersion());
        if (run == null) {
            // Untagged/homebuilt build; cannot judge, so do not block plugins.
            return true;
        }

        return run.major == requiredVersion.major
                && (run.minor > requiredVersion.minor
                || (run.minor == requiredVersion.minor && run.patch >= requiredVersion.patch));
    }

    private static @Nullable Minor parse(@Nullable String version) {
        if (version == null) {
            return null;
        }
        Matcher matcher = VERSION_PATTERN.matcher(version.trim());
        if (!matcher.find()) {
            return null;
        }
        try {
            return new Minor(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3))
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record Minor(int major, int minor, int patch) {
    }
}
