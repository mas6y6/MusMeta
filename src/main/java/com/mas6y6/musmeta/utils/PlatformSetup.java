package com.mas6y6.musmeta.utils;

public final class PlatformSetup {
    private static boolean macConfigured;

    private PlatformSetup() {
    }

    public static void applyMacPlatformSettings() {
        if (macConfigured || !isMac()) {
            return;
        }
        macConfigured = true;

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "MusMeta");
        System.setProperty("apple.awt.application.appearance", "system");
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }
}