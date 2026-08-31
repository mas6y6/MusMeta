package com.mas6y6.musmeta.utils;

import com.mas6y6.musmeta.ui.album.AlbumUI;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Utils {
    private static final int ARTWORK_SIZE = 180;

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
}
