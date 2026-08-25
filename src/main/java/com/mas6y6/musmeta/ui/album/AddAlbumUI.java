package com.mas6y6.musmeta.ui.album;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mas6y6.musmeta.ui.components.album.AlbumArtwork;
import com.mas6y6.musmeta.ui.dialogs.EXTDialog;
import com.mas6y6.musmeta.utils.ColorWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class AddAlbumUI extends JPanel {
    private static final int ARTWORK_SIZE = 180;
    private static final Color HOVER_TINT = new Color(255, 255, 255, 70);
    private static final int HOVER_PADDING = 20;

    private final AlbumArtwork artwork;

    public AddAlbumUI() {
        super(new BorderLayout(0, 6));

        setOpaque(false);

        setBorder(
                BorderFactory.createEmptyBorder(
                        HOVER_PADDING,
                        HOVER_PADDING,
                        HOVER_PADDING,
                        HOVER_PADDING
                )
        );

        setPreferredSize(
                new Dimension(
                        ARTWORK_SIZE + HOVER_PADDING * 2,
                        240 + HOVER_PADDING * 2
                )
        );

        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(
                        Cursor.getPredefinedCursor(
                                Cursor.HAND_CURSOR
                        )
                );

                setHovered(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(
                        Cursor.getDefaultCursor()
                );

                setHovered(false);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // TODO: newAlbum();

                    EXTDialog.showMessageDialog(
                            e.getComponent(),
                            "In Development",
                            "In Development",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                }
            }
        };

        addMouseListener(mouseAdapter);

        // Artwork
        ImageIcon originalIcon = new FlatSVGIcon(
                Objects.requireNonNull(
                        getClass().getResource("/plus.svg")
                )
        );

        Image scaledImage = originalIcon.getImage()
                .getScaledInstance(
                        32,
                        32,
                        Image.SCALE_SMOOTH
                );

        artwork = new AlbumArtwork(
                new ImageIcon(scaledImage),
                10
        );

        artwork.setPreferredSize(
                new Dimension(
                        ARTWORK_SIZE,
                        ARTWORK_SIZE
                )
        );

        artwork.addMouseListener(mouseAdapter);

        add(artwork, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        int padding_d = 20;

        g2.setColor(ColorWrapper.addWrapper(getBackground()).darker(0.80f));
        g2.fillRoundRect(
                padding_d,
                padding_d,
                getWidth() - padding_d * 2,
                getHeight() - padding_d * 2,
                12,
                12
        );

        if (!hovered) {
            return;
        }

        try {
            int padding = 4;

            int width = getWidth() - padding * 2;
            int height = getHeight() - padding * 2;

            g2.setColor(HOVER_TINT);
            g2.fillRoundRect(
                    padding,
                    padding,
                    width,
                    height,
                    12,
                    12
            );
        } finally {
            g2.dispose();
        }
    }

    private boolean hovered = false;

    private void setHovered(boolean hovered) {
        if (this.hovered != hovered) {
            this.hovered = hovered;
            repaint();
        }
    }
}
