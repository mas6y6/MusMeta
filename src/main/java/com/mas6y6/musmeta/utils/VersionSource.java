package com.mas6y6.musmeta.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class VersionSource {
    public record Version(String githubCommit, String musmetaVersion, boolean isHomebuilt) {}

    private static final String UNKNOWN = "unknown";
    private static final String RESOURCE = "/musmeta.properties";

    private static final Properties PROPERTIES = loadProperties();
    private static final String VERSION = PROPERTIES.getProperty("version", UNKNOWN);

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream input = VersionSource.class.getResourceAsStream(RESOURCE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load MusMeta version", e);
        }

        return properties;
    }

    private VersionSource() {
    }

    public static String getString() {
        return VERSION;
    }

    public static Version getVersion() {
        if (VERSION.equals(UNKNOWN)) {
            throw new RuntimeException("Unable to load MusMeta version");
        }

        String githubCommit = PROPERTIES.getProperty("githubCommit", "").trim();
        String musmetaVersion = PROPERTIES.getProperty("musmetaVersion", VERSION).trim();
        boolean isHomebuilt = Boolean.parseBoolean(PROPERTIES.getProperty("homebuilt", "true").trim());

        return new Version(
                githubCommit.isEmpty() ? null : githubCommit,
                musmetaVersion.isEmpty() ? UNKNOWN : musmetaVersion,
                isHomebuilt
        );
    }
}
