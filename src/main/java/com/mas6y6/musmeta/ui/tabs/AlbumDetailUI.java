package com.mas6y6.musmeta.ui.tabs;

import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.ui.MainWindow;
import com.mas6y6.musmeta.ui.components.album.AlbumArtwork;
import com.mas6y6.musmeta.ui.dialogs.EditAlbumDialog;
import com.mas6y6.musmeta.ui.dialogs.EditSongDialog;
import com.mas6y6.musmeta.ui.dialogs.base.EXTDialog;
import org.jaudiotagger.tag.FieldKey;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An iTunes-style detailed view of a single album, opened as its own tab.
 * Shows the artwork, album metadata and a list of its tracks.
 */
public class AlbumDetailUI extends JPanel {

    private static final int ARTWORK_SIZE = 260;

    private final Album album;
    private boolean manyArtists = false;

    private final List<Song> selectedSongs = new ArrayList<>();
    private DefaultTableModel tableModel;

    private Consumer<List<Song>> selectionListener;

    public AlbumDetailUI(Album album) {
        super(new BorderLayout());
        this.album = album;

        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        add(header(), BorderLayout.NORTH);
        add(trackTable(), BorderLayout.CENTER);
    }

    public Album getAlbum() {
        return album;
    }

    public void refresh() {
        removeAll();
        add(header(), BorderLayout.NORTH);
        add(trackTable(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void selectAllTracks() {
        if (tableModel != null) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(true, i, 0);
            }
        }
    }

    public void deselectAllTracks() {
        if (tableModel != null) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(false, i, 0);
            }
        }
    }

    public void setSelectionListener(Consumer<List<Song>> selectionListener) {
        this.selectionListener = selectionListener;
    }

    public List<Song> getSelectedSongs() {
        return List.copyOf(selectedSongs);
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setOpaque(false);

        AlbumArtwork artwork = new AlbumArtwork(12);
        artwork.setPreferredSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));
        artwork.setMaximumSize(artwork.getPreferredSize());
        artwork.setArtwork(album.getArtworkImage());
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

        meta.add(Box.createVerticalStrut(4));

        JButton editbutton = new JButton("Edit Album Info");
        editbutton.setAlignmentX(Component.LEFT_ALIGNMENT);
        editbutton.addActionListener((event) -> {
            new EditAlbumDialog(MainWindow.INSTANCE, album).setVisible(true);
        });

        meta.add(editbutton);

        meta.add(Box.createVerticalStrut(8));

        header.add(meta, BorderLayout.CENTER);

        return header;
    }

    private String subtitle() {
        List<Song> songs = album.getSongs();
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

        String year = album.getYear();

        List<String> parts = new ArrayList<>();
        if (year != null && !year.isBlank()) {
            parts.add(year);
        }
        parts.add(tracks + " songs");
        parts.add(formatDuration(seconds));

        String line = String.join("  •  ", parts);
        String artist = artistLabel();
        if (artist != null && !artist.isBlank()) {
            return artist + "  •  " + line;
        }
        return line;
    }

    /**
     * Returns the artist shown for the album: the shared track artist when all
     * songs come from the same artist, or "Various Artists" when the album
     * contains tracks from different artists.
     */
    private String artistLabel() {
        return album.getArtist().artist();
    }

    private JScrollPane trackTable() {
        String[] columns = {"", "#", "Title", "Artist", "Time"};

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : super.getColumnClass(columnIndex);
            }
        };

        reloadTrackTableData();

        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < album.getSongs().size()) {
                        new EditSongDialog(MainWindow.INSTANCE, album.getSongs().get(row)).setVisible(true);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowMenu(e);
            }

            private void maybeShowMenu(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && row < album.getSongs().size()) {
                    handleRightClickMenu(e, album.getSongs().get(row));
                } else {
                    handleRightClickMenu(e, null);
                }
            }
        });

        tableModel.addTableModelListener(e -> {
            if (e.getColumn() != 0) {
                return;
            }
            selectedSongs.clear();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                if (Boolean.TRUE.equals(tableModel.getValueAt(row, 0))) {
                    selectedSongs.add(album.getSongs().get(row));
                }
            }
            if (selectionListener != null) {
                selectionListener.accept(getSelectedSongs());
            }
        });

        if (table.getColumnCount() > 3) {
            table.getColumnModel().getColumn(0).setMaxWidth(32);
            table.getColumnModel().getColumn(1).setPreferredWidth(40);
            table.getColumnModel().getColumn(2).setPreferredWidth(320);
            table.getColumnModel().getColumn(3).setPreferredWidth(180);
            table.getColumnModel().getColumn(4).setPreferredWidth(60);
        }

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(right);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        return scrollPane;
    }

    public void reloadTrackTable() {
        if (tableModel != null) {
            selectedSongs.clear();
            reloadTrackTableData();
        }
    }

    private void reloadTrackTableData() {
        if (tableModel == null) {
            return;
        }

        tableModel.setRowCount(0);
        List<Song> songs = album.getSongs();
        for (Song song : songs) {
            String number = song.getTrackNumber() > 0
                    ? String.valueOf(song.getTrackNumber())
                    : "";
            tableModel.addRow(new Object[]{
                    false,
                    number,
                    song.getTitle(),
                    song.getArtist(),
                    trackLength(song)
            });
        }
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

    private void handleRightClickMenu(MouseEvent mouseEvent, Song clickedSong) {
        var popupMenu = new JPopupMenu();

        List<Song> songsToEdit;
        if (!selectedSongs.isEmpty() && (clickedSong == null || selectedSongs.contains(clickedSong))) {
            songsToEdit = selectedSongs;
        } else if (clickedSong != null) {
            songsToEdit = List.of(clickedSong);
        } else {
            songsToEdit = List.of();
        }

        if (!songsToEdit.isEmpty()) {
            String label = songsToEdit.size() > 1
                    ? "Get Info (" + songsToEdit.size() + " Songs)..."
                    : "Get Info...";
            JMenuItem editSong = new JMenuItem(label);
            editSong.addActionListener(e -> new EditSongDialog(MainWindow.INSTANCE, songsToEdit).setVisible(true));
            popupMenu.add(editSong);
            popupMenu.addSeparator();
        }

        JMenuItem editAlbum = new JMenuItem("Edit Album Info...");
        editAlbum.addActionListener(e -> new EditAlbumDialog(MainWindow.INSTANCE, album).setVisible(true));
        popupMenu.add(editAlbum);

        popupMenu.addSeparator();

        JMenuItem selectAll = new JMenuItem("Select All");
        selectAll.addActionListener(e -> selectAllTracks());
        popupMenu.add(selectAll);

        JMenuItem deselectAll = new JMenuItem("Deselect All");
        deselectAll.addActionListener(e -> deselectAllTracks());
        popupMenu.add(deselectAll);

        if (!songsToEdit.isEmpty()) {
            popupMenu.addSeparator();

            JMenuItem delete = new JMenuItem("Delete");
            delete.addActionListener(e -> {
                if (
                        EXTDialog.showOptionDialog(
                                MainWindow.INSTANCE,
                                "Do you want to delete the selected song(s)?",
                                "Delete Song(s)?",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                null,
                                null
                        )
                                == JOptionPane.YES_OPTION) {
                    songsToEdit.forEach(Library.getInstance()::removeSong);
                    reloadTrackTable();
                    MainWindow.INSTANCE.getLibraryUI().refresh();
                };
            });
            popupMenu.add(delete);
        }

        if (mouseEvent.getComponent() instanceof JTable table) {
            popupMenu.show(table, mouseEvent.getX(), mouseEvent.getY());
        } else {
            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }
    }
}
