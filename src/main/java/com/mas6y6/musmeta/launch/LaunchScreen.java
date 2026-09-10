package com.mas6y6.musmeta.launch;

import com.formdev.flatlaf.FlatClientProperties;
import com.mas6y6.musmeta.utils.Version;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public final class LaunchScreen extends JWindow {
    private static final int WIDTH = 640;
    private static final Color BACKGROUND = new Color(0x18, 0x18, 0x18);
    private static final Color BORDER = new Color(0x3A, 0x3A, 0x3A);
    private static final Color STATUS_COLOR = new Color(0xC8, 0xC8, 0xC8);
    private static final Color ACCENT = new Color(255, 128, 134);

    private static volatile LaunchScreen instance;

    private final JLabel statusLabel;

    private LaunchScreen() {
        setBackground(BACKGROUND);
        setAlwaysOnTop(true);

        JLabel banner = createBanner();
        banner.setHorizontalAlignment(SwingConstants.CENTER);
        banner.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8, 0)
        );

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(
                new BoxLayout(textPanel, BoxLayout.Y_AXIS)
        );
        textPanel.setBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );

        JLabel version = new JLabel("v" + Version.get());
        version.setFont(loadFont().deriveFont(13f));
        version.setForeground(ACCENT);
        version.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel("Starting MusMeta...");
        statusLabel.setFont(loadFont().deriveFont(12f));
        statusLabel.setForeground(STATUS_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(BACKGROUND);
        progressBar.setForeground(ACCENT);
        progressBar.setPreferredSize(new Dimension(180, 5));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(version);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(statusLabel);
        textPanel.add(Box.createVerticalStrut(12));
        textPanel.add(progressBar);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        content.add(banner, BorderLayout.NORTH);
        content.add(textPanel, BorderLayout.CENTER);

        setContentPane(content);
        pack();
        setLocationRelativeTo(null);
        setShape(new RoundRectangle2D.Double(
                0, 0, getWidth(), getHeight(), 56, 56
        ));
    }

    private static JLabel createBanner() {
        Image image = loadBanner();
        if (image != null) {
            return new JLabel(new ImageIcon(image));
        }
        JLabel fallback = new JLabel("MusMeta", SwingConstants.CENTER);
        fallback.setFont(loadFont().deriveFont(Font.BOLD, 48f));
        fallback.setForeground(ACCENT);
        return fallback;
    }

    private static Image loadBanner() {
        try (InputStream in = LaunchScreen.class
                .getResourceAsStream("/banner.png")) {
            if (in == null) {
                return null;
            }
            Image source = Toolkit.getDefaultToolkit()
                    .createImage(in.readAllBytes());
            MediaTracker tracker = new MediaTracker(new Canvas());
            tracker.addImage(source, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int bannerHeight = (int) (
                    (double) source.getHeight(null)
                            / source.getWidth(null)
                            * WIDTH
            );
            BufferedImage scaled = new BufferedImage(
                    WIDTH, bannerHeight, BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
            g2d.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            g2d.drawImage(source, 0, 0, WIDTH, bannerHeight, null);
            g2d.dispose();
            return scaled;
        } catch (Exception e) {
            return null;
        }
    }

    private static Font loadFont() {
        try (InputStream in = LaunchScreen.class
                .getResourceAsStream("/font/Outfit-Medium.ttf")) {
            if (in != null) {
                return Font.createFont(Font.TRUETYPE_FONT, in);
            }
        } catch (Exception ignored) {
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

    public static void showSplash() {
        SwingUtilities.invokeLater(() -> {
            if (instance == null) {
                instance = new LaunchScreen();
            }
            instance.setVisible(true);
        });
    }

    public static void setStatus(String status) {
        LaunchScreen current = instance;
        if (current == null) {
            return;
        }
        SwingUtilities.invokeLater(() ->
                current.statusLabel.setText(status)
        );
    }

    public static void closeIfOpen() {
        LaunchScreen current = instance;
        if (current != null) {
            current.dispose();
        }
    }
}