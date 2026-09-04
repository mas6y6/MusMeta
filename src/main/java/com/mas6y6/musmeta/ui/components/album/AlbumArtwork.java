package com.mas6y6.musmeta.ui.components.album;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class AlbumArtwork extends JLabel {

    private static final int MASTER_OVERSAMPLE = 2;
    private static final int MAX_DEVICE_SIZE = 4096;

    private final int radius;

    private Image source;

    private boolean sourceBounded;

    private BufferedImage cache;
    private int cacheWidth;
    private int cacheHeight;

    public AlbumArtwork(Icon icon, int radius) {
        super(icon, SwingConstants.CENTER);
        this.radius = radius;
        setOpaque(false);
    }

    public AlbumArtwork(int radius) {
        super("" ,SwingConstants.CENTER);
        this.radius = radius;
        setOpaque(false);
    }

    public AlbumArtwork(String text, int radius) {
        super(text ,SwingConstants.CENTER);
        this.radius = radius;
        setOpaque(false);
    }

    public void setArtwork(Image artwork) {
        this.source = artwork;
        this.sourceBounded = false;
        this.cache = null;

        if (artwork != null && isPreferredSizeSet()) {
            Dimension size = getPreferredSize();

            if (size.width > 0 && size.height > 0) {
                this.source = boundedCopy(artwork, size.width, size.height);
                this.sourceBounded = true;
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            BufferedImage artwork = artwork(g2, width, height);

            if (artwork != null) {
                g2.drawImage(artwork, 0, 0, width, height, null);
            }

            g2.clip(
                    new RoundRectangle2D.Float(
                            0,
                            0,
                            width,
                            height,
                            radius,
                            radius
                    )
            );

            super.paintComponent(g2);

        } finally {
            g2.dispose();
        }
    }

    private BufferedImage artwork(Graphics2D g2, int width, int height) {
        if (source == null) {
            return null;
        }

        AffineTransform transform = g2.getTransform();

        double scaleX = Math.hypot(
                transform.getScaleX(),
                transform.getShearY()
        );
        double scaleY = Math.hypot(
                transform.getShearX(),
                transform.getScaleY()
        );

        if (scaleX <= 0 || scaleY <= 0) {
            scaleX = 1;
            scaleY = 1;
        }

        int deviceWidth = clampDeviceSize(width * scaleX);
        int deviceHeight = clampDeviceSize(height * scaleY);

        if (cache != null
                && cacheWidth == deviceWidth
                && cacheHeight == deviceHeight) {
            return cache;
        }

        if (!sourceBounded) {
            source = boundedCopy(source, deviceWidth, deviceHeight);
            sourceBounded = true;
        }

        if (source == null) {
            return null;
        }

        cache = render(
                source,
                deviceWidth,
                deviceHeight,
                (float) (radius * scaleX),
                (float) (radius * scaleY)
        );

        cacheWidth = deviceWidth;
        cacheHeight = deviceHeight;

        return cache;
    }

    private static int clampDeviceSize(double size) {
        return (int) Math.clamp(Math.round(size), 1,
                MAX_DEVICE_SIZE);
    }

    private static Image boundedCopy(Image image, int deviceWidth, int deviceHeight) {
        BufferedImage cropped = crop(image, deviceWidth, deviceHeight);

        if (cropped == null) {
            return null;
        }

        int maxWidth = deviceWidth * MASTER_OVERSAMPLE;
        int maxHeight = deviceHeight * MASTER_OVERSAMPLE;

        if (cropped.getWidth() > maxWidth || cropped.getHeight() > maxHeight) {
            return scale(cropped, maxWidth, maxHeight);
        }

        return cropped;
    }

    private static BufferedImage render(
            Image source,
            int width,
            int height,
            float arcWidth,
            float arcHeight
    ) {
        BufferedImage cropped = crop(source, width, height);

        if (cropped == null) {
            return null;
        }

        BufferedImage result = scale(cropped, width, height);

        if (arcWidth > 0 && arcHeight > 0) {
            applyRoundedMask(result, arcWidth, arcHeight);
        }

        return result;
    }

    private static BufferedImage crop(Image image, int targetWidth, int targetHeight) {
        BufferedImage buffered = toBufferedImage(image);

        if (buffered == null) {
            return null;
        }

        int width = buffered.getWidth();
        int height = buffered.getHeight();

        int cropWidth = Math.clamp(
                width, 1,
                (int) Math.round(height * (double) targetWidth / targetHeight)
        );
        int cropHeight = Math.clamp(
                height, 1,
                (int) Math.round(width * (double) targetHeight / targetWidth)
        );

        if (cropWidth == width && cropHeight == height) {
            return buffered;
        }

        return buffered.getSubimage(
                (width - cropWidth) / 2,
                (height - cropHeight) / 2,
                cropWidth,
                cropHeight
        );
    }

    private static BufferedImage scale(BufferedImage image, int targetWidth, int targetHeight) {
        BufferedImage current = image;
        int width = current.getWidth();
        int height = current.getHeight();

        while (width > 1
                && height > 1
                && width / 2 >= targetWidth
                && height / 2 >= targetHeight) {
            width = Math.max(1, width / 2);
            height = Math.max(1, height / 2);

            current = drawScaled(
                    current,
                    width,
                    height,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
        }

        if (current != image && width == targetWidth && height == targetHeight) {
            return current;
        }

        return drawScaled(
                current,
                targetWidth,
                targetHeight,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );
    }

    private static BufferedImage drawScaled(
            Image image,
            int width,
            int height,
            Object interpolation
    ) {
        BufferedImage result = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB_PRE
        );

        Graphics2D g2 = result.createGraphics();

        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    interpolation
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.drawImage(image, 0, 0, width, height, null);

        } finally {
            g2.dispose();
        }

        return result;
    }

    private static void applyRoundedMask(BufferedImage image, float arcWidth, float arcHeight) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage mask = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB_PRE
        );

        Graphics2D maskGraphics = mask.createGraphics();

        try {
            maskGraphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            maskGraphics.setColor(Color.WHITE);
            maskGraphics.fill(
                    new RoundRectangle2D.Float(
                            0,
                            0,
                            width,
                            height,
                            arcWidth,
                            arcHeight
                    )
            );

        } finally {
            maskGraphics.dispose();
        }

        Graphics2D g2 = image.createGraphics();

        try {
            g2.setComposite(AlphaComposite.DstIn);
            g2.drawImage(mask, 0, 0, null);

        } finally {
            g2.dispose();
        }
    }

    private static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage buffered) {
            return buffered;
        }

        int width = image.getWidth(null);
        int height = image.getHeight(null);

        if (width <= 0 || height <= 0) {
            return null;
        }

        BufferedImage result = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB_PRE
        );

        Graphics2D g2 = result.createGraphics();

        try {
            g2.drawImage(image, 0, 0, null);

        } finally {
            g2.dispose();
        }

        return result;
    }
}
