package com.mas6y6.musmeta.ui.album;

import com.mas6y6.musmeta.ui.components.RoundedLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

public class AlbumUI extends JPanel {

    private static final int ARTWORK_SIZE = 180;

    private final RoundedLabel artwork;
    private final JLabel title;
    private final JLabel artist;

    public AlbumUI(String album, String artist) {
        super(new BorderLayout(0, 6));

        setOpaque(false);
        setPreferredSize(new Dimension(ARTWORK_SIZE, 240));

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
                    // TODO: openAlbum();
                }
            }
        };

        addMouseListener(mouseAdapter);

        // Artwork
        ImageIcon originalIcon = new ImageIcon(
                Objects.requireNonNull(
                        getClass().getResource("/testalbum.jpg")
                )
        );

        Image scaledImage = originalIcon.getImage()
                .getScaledInstance(
                        ARTWORK_SIZE,
                        ARTWORK_SIZE,
                        Image.SCALE_SMOOTH
                );

        artwork = new RoundedLabel(
                new ImageIcon(scaledImage),
                10
        );

        artwork.setPreferredSize(
                new Dimension(
                        ARTWORK_SIZE,
                        ARTWORK_SIZE
                )
        );

        add(artwork, BorderLayout.NORTH);

        // Text container
        JPanel text = new JPanel();

        text.setOpaque(false);

        text.setLayout(
                new BoxLayout(
                        text,
                        BoxLayout.Y_AXIS
                )
        );

        // Album title
        title = new JLabel(album);

        title.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        title.setFont(
                title.getFont().deriveFont(
                        Font.BOLD,
                        14f
                )
        );

        // Artist
        this.artist = new JLabel(artist);

        this.artist.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        this.artist.setFont(
                this.artist.getFont().deriveFont(
                        Font.PLAIN,
                        13f
                )
        );

        this.artist.setForeground(
                UIManager.getColor(
                        "Label.disabledForeground"
                )
        );

        text.add(title);
        text.add(this.artist);

        add(text, BorderLayout.CENTER);
    }

    private boolean hovered = false;

    private void setHovered(boolean hovered) {
        if (this.hovered != hovered) {
            this.hovered = hovered;
            repaint();
        }
    }
}