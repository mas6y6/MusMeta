package com.mas6y6.musmeta.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Version {

    private static final String VERSION;

    static {
        Properties properties = new Properties();

        try (InputStream input = Version.class
                .getResourceAsStream("/musmeta.properties")) {

            if (input == null) {
                VERSION = "unknown";
            } else {
                properties.load(input);
                VERSION = properties.getProperty("version", "unknown");
            }

        } catch (IOException e) {
            throw new RuntimeException("Unable to load MusMeta version", e);
        }
    }

    private Version() {
    }

    public static String get() {
        return VERSION;
    }
}