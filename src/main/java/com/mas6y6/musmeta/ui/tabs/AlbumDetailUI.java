package com.mas6y6.musmeta.ui.tabs;

import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.ui.components.album.AlbumArtwork;
import org.jaudiotagger.tag.FieldKey;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * An iTunes-style detailed view of a single album, opened as its own tab.
 * Shows the artwork, album metadata and a list of its tracks.
 */
public class AlbumDetailUI extends JPanel {

    private static final int ARTWORK_SIZE = 260;

    private final Album album;

    public AlbumDetailUI(Album album) {
        super(new BorderLayout());
        this.album = album;

        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        add(header(), BorderLayout.NORTH);
        add(trackTable(), BorderLayout.CENTER);
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setOpaque(false);

        AlbumArtwork artwork = new AlbumArtwork(
                prepareArtwork(),
                12
        );
        artwork.setPreferredSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));
        artwork.setMaximumSize(artwork.getPreferredSize());
        header.add(artwork, BorderLayout.WEST);

        // Metadata column
        JPanel meta = new JPanel();
        meta.setOpaque(false);
        meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
        meta.setAlignmentY(Component.TOP_ALIGNMENT);

        WrappingLabel title = new WrappingLabel(album.getTitle());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        meta.add(title);

        meta.add(Box.createVerticalStrut(4));

        JLabel artistLine = new JLabel(subtitle());
        artistLine.setFont(artistLine.getFont().deriveFont(Font.PLAIN, 15f));
        artistLine.setForeground(UIManager.getColor("Label.disabledForeground"));
        artistLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        meta.add(artistLine);

        meta.add(Box.createVerticalStrut(12));

        header.add(meta, BorderLayout.CENTER);

        return header;
    }

    private String subtitle() {
        List<Song> songs = album.getSongs();
        String artist = album.getArtist();
        int tracks = songs.size();
        int seconds = 0;
        for (Song song : songs) {
            try {
                int length = song.getAudioFile().getAudioHeader().getTrackLength();
                if (length > 0) {
                    seconds += length;
                }
            } catch (Exception ignored) {
                // Some formats do not expose a duration.
            }
        }

        String year = "";
        if (!songs.isEmpty()) {
            try {
                year = songs.get(0).getTag().getFirst(FieldKey.YEAR);
            } catch (Exception ignored) {
                // Some tag types do not support a year field.
            }
        }

        List<String> parts = new ArrayList<>();
        if (year != null && !year.isBlank()) {
            parts.add(year);
        }
        parts.add(tracks + " songs");
        parts.add(formatDuration(seconds));

        String line = String.join("  •  ", parts);
        if (artist != null && !artist.isBlank()) {
            return artist + "  •  " + line;
        }
        return line;
    }

    private JScrollPane trackTable() {
        String[] columns = {"#", "Title", "Artist", "Time"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        List<Song> songs = album.getSongs();
        for (Song song : songs) {
            String number = song.getTrackNumber() > 0
                    ? String.valueOf(song.getTrackNumber())
                    : "";
            model.addRow(new Object[]{
                    number,
                    song.getTitle(),
                    song.getArtist(),
                    trackLength(song)
            });
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (table.getColumnCount() > 3) {
            table.getColumnModel().getColumn(0).setPreferredWidth(40);
            table.getColumnModel().getColumn(1).setPreferredWidth(320);
            table.getColumnModel().getColumn(2).setPreferredWidth(180);
            table.getColumnModel().getColumn(3).setPreferredWidth(60);
        }

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(right);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        return scrollPane;
    }

    private String trackLength(Song song) {
        try {
            int length = song.getAudioFile().getAudioHeader().getTrackLength();
            if (length > 0) {
                return formatDuration(length);
            }
        } catch (Exception ignored) {
            // No duration available.
        }
        return "";
    }

    private static String formatDuration(int seconds) {
        if (seconds <= 0) {
            return "0:00";
        }
        int minutes = seconds / 60;
        int sec = seconds % 60;
        return minutes + ":" + (sec < 10 ? "0" : "") + sec;
    }

    private ImageIcon prepareArtwork() {
        Image image = album.getArtworkImage();
        if (image == null) {
            return null;
        }
        return new ImageIcon(centerCrop(image, ARTWORK_SIZE));
    }

    /**
     * Center-crops the source to a square and scales it down (never up beyond
     * the original's smaller dimension) to the given size using bicubic
     * interpolation, preserving as much detail as the source provides.
     */
    private static BufferedImage centerCrop(Image image, int size) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width <= 0 || height <= 0) {
            return null;
        }

        int side = Math.min(width, height);
        int sx = (width - side) / 2;
        int sy = (height - side) / 2;

        BufferedImage out = new BufferedImage(
                size,
                size,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = out.createGraphics();
        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            g2.drawImage(
                    image,
                    0,
                    0,
                    size,
                    size,
                    sx,
                    sy,
                    sx + side,
                    sy + side,
                    null
            );
        } finally {
            g2.dispose();
        }

        return out;
    }

    /**
     * A label that word-wraps its text across multiple lines when its text is
     * wider than the available space (the width of its parent container), so
     * long album titles wrap instead of overflowing horizontally.
     */
    private static final class WrappingLabel extends JLabel {
        private WrappingLabel(String text) {
            super(text);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension single = super.getPreferredSize();
            int available = availableWidth();
            if (available <= 0 || single.width <= available) {
                return single;
            }

            FontMetrics fm = getFontMetrics(getFont());
            StringBuilder current = new StringBuilder();
            int lines = 1;
            for (String word : getText().split("\\s+")) {
                String probe = current.length() == 0
                        ? word
                        : current + " " + word;
                if (fm.stringWidth(probe) > available && current.length() > 0) {
                    lines++;
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(probe);
                }
            }

            return new Dimension(available, fm.getHeight() * lines);
        }

        private int availableWidth() {
            if (getParent() == null) {
                return -1;
            }
            Insets insets = getParent().getInsets();
            return getParent().getWidth() - insets.left - insets.right;
        }
    }
}
