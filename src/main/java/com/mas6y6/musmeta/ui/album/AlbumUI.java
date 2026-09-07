package com.mas6y6.musmeta.ui.album;

import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.ui.MainWindow;
import com.mas6y6.musmeta.ui.components.album.AlbumArtwork;
import com.mas6y6.musmeta.ui.dialogs.EditAlbumDialog;
import com.mas6y6.musmeta.ui.dialogs.base.EXTDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class AlbumUI extends JPanel {

    private static final int ARTWORK_SIZE = 180;
    private static final Color HOVER_TINT = new Color(255, 255, 255, 70);
    private static final int HOVER_PADDING = 20;

    private final Album album;
    private final AlbumArtwork artwork;
    private final JLabel title;
    private final JLabel artist;

    public AlbumUI(Album album, Image artworkImage) {
        super(new BorderLayout(0, 6));
        this.album = album;

        String artist = album.getArtist().artist();
        if (album.hasDiscs()) {
            artist = artist + "  •  " + album.getDiscs().size() + " discs";
        }

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
                    MainWindow.INSTANCE.openAlbumTab(album);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClickMenu(e);
                }
            }
        };

        addMouseListener(mouseAdapter);

        // Artwork

        artwork = new AlbumArtwork(10);

        artwork.setPreferredSize(
                new Dimension(
                        ARTWORK_SIZE,
                        ARTWORK_SIZE
                )
        );

        artwork.setArtwork(
                artworkImage != null ? artworkImage : placeholderArtwork()
        );

        artwork.addMouseListener(mouseAdapter);

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
        title = new JLabel(album.getTitle());

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

        text.addMouseListener(mouseAdapter);
        title.addMouseListener(mouseAdapter);
        this.artist.addMouseListener(mouseAdapter);

        add(text, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (!hovered) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics.create();

        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

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

    private static Image placeholderArtwork() {
        return new ImageIcon(
                Objects.requireNonNull(
                        AlbumUI.class.getResource("/placeholder_album.png")
                )
        ).getImage();
    }

    public Album getAlbum() {
        return album;
    }

    private void handleRightClickMenu(MouseEvent mouseEvent) {
        var popupMenu = new JPopupMenu();

        var openInNewTab = new JMenuItem("Open In New Tab");
        popupMenu.add(openInNewTab);
        openInNewTab.addActionListener(e -> {MainWindow.INSTANCE.openAlbumTab(album);});

        var editAlbum = new JMenuItem("Edit Album");
        editAlbum.addActionListener(e -> {
            new EditAlbumDialog(MainWindow.INSTANCE, album).setVisible(true);
        });
        popupMenu.add(editAlbum);

        var deleteAlbum = new JMenuItem("Delete Album");
        deleteAlbum.addActionListener(e -> {
             if (
                EXTDialog.showOptionDialog(
                    MainWindow.INSTANCE,
                    "Are you sure you want to delete this album?",
                    "Delete Album?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    null
                )
                     == JOptionPane.YES_OPTION) {
                 Library.getInstance().removeAlbum(album.getTitle());
             };
        });
        popupMenu.add(deleteAlbum);

        popupMenu.show(this, mouseEvent.getX(), mouseEvent.getY());
    }
}
