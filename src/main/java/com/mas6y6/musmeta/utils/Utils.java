package com.mas6y6.musmeta.utils;

import com.mas6y6.musmeta.ui.album.AlbumUI;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

public class Utils {
    private static final int ARTWORK_SIZE = 180;

    private Utils() {}

    public static boolean isRunningAsRoot() {
        String user = System.getProperty("user.name");
        if ("root".equalsIgnoreCase(user)) {
            return true;
        }
        String uid = System.getenv("UID");
        if ("0".equals(uid)) {
            return true;
        }
        try {
            Process process = new ProcessBuilder("id", "-u")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.waitFor() == 0 && "0".equals(output)) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static BufferedImage prepareArtwork(Image image) {
        if (image == null) {
            image = new ImageIcon(
                    Objects.requireNonNull(
                            AlbumUI.class.getResource("/placeholder_album.png")
                    )
            ).getImage();
        }

        int width = image.getWidth(null);
        int height = image.getHeight(null);
        int size = Math.min(width, height);

        BufferedImage result = new BufferedImage(
                ARTWORK_SIZE,
                ARTWORK_SIZE,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = result.createGraphics();

        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            int x = (width - size) / 2;
            int y = (height - size) / 2;

            g2.drawImage(
                    image,
                    0,
                    0,
                    ARTWORK_SIZE,
                    ARTWORK_SIZE,
                    x,
                    y,
                    x + size,
                    y + size,
                    null
            );
        } finally {
            g2.dispose();
        }

        return result;
    }

    private static final Set<String> WINDOWS_RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    );


    public static String toSafeFilename(String input) {
        if (input == null || input.isBlank()) {
            return "untitled";
        }

        String name = input
                .replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]", "_")
                .replaceAll("[ .]+$", "");

        if (name.equals(".") || name.equals("..") || name.isBlank()) {
            return "untitled";
        }

        String baseName = name;
        int dot = baseName.indexOf('.');
        if (dot >= 0) {
            baseName = baseName.substring(0, dot);
        }

        if (WINDOWS_RESERVED_NAMES.contains(baseName.toUpperCase())) {
            name = "_" + name;
        }

        return name;
    }
}
