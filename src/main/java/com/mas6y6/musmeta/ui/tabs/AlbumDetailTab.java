package com.mas6y6.musmeta.ui.tabs;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Disc;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.musicplayer.MusicPlayer;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.MainWindow;
import com.mas6y6.musmeta.ui.components.album.AlbumArtwork;
import com.mas6y6.musmeta.ui.dialogs.*;
import com.mas6y6.musmeta.ui.dialogs.base.MDialog;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;
import com.mas6y6.musmeta.utils.FFmpegUtils;
import org.jaudiotagger.tag.FieldKey;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AlbumDetailTab extends JPanel {

    private static final int CONTENT_PADDING_H = 40;
    private static final int CONTENT_PADDING_V = 30;

    private static final int ARTWORK_SIZE = 260;

    private final Album album;

    private final List<Song> selectedSongs = new ArrayList<>();
    private final List<DiscTable> discTables = new ArrayList<>();

    private final AlbumArtwork artwork = new AlbumArtwork(12);
    private Image artworkImage;

    private Consumer<List<Song>> selectionListener;

    private final AtomicBoolean updating = new AtomicBoolean(false);
    private final AtomicBoolean bulkSelecting = new AtomicBoolean(false);

    public AlbumDetailTab(Album album) {
        super(new BorderLayout());
        this.album = album;

        add(scrollableContent(), BorderLayout.CENTER);
    }

    public Album getAlbum() {
        return album;
    }

    public void refresh() {
        removeAll();
        add(scrollableContent(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void selectAllTracks() {
        setAllTracksChecked(true);
    }

    public void deselectAllTracks() {
        setAllTracksChecked(false);
    }

    private void setAllTracksChecked(boolean checked) {
        bulkSelecting.set(true);
        try {
            for (DiscTable discTable : discTables) {
                DefaultTableModel model = discTable.model();
                for (int row = 0; row < model.getRowCount(); row++) {
                    model.setValueAt(checked, row, 0);
                }
            }
        } finally {
            bulkSelecting.set(false);
        }
        rebuildSelection();
    }

    public void setSelectionListener(Consumer<List<Song>> selectionListener) {
        this.selectionListener = selectionListener;
    }

    public List<Song> getSelectedSongs() {
        return List.copyOf(selectedSongs);
    }

    private void reformatSongs() {
        List<Song> allSongs = album.getSongs();
        if (allSongs.isEmpty()) {
            MDialog.showMessageDialog(
                    MainWindow.INSTANCE,
                    "This album has no songs to reformat.",
                    "Reformat Songs",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (FFmpegUtils.getFFmpegExecutable() == null) {
            MDialog.showMessageDialog(
                    MainWindow.INSTANCE,
                    "FFmpeg is required to convert audio formats.\n"
                            + "Set up an FFmpeg binary in Settings > FFmpeg first.",
                    "FFmpeg Required",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        List<Song> selected = getSelectedSongs();
        JComboBox<String> scopePicker = new JComboBox<>();
        if (selected.isEmpty()) {
            scopePicker.addItem("All songs in album (" + allSongs.size() + ")");
        } else {
            scopePicker.addItem("Selected songs (" + selected.size() + ")");
            scopePicker.addItem("All songs in album (" + allSongs.size() + ")");
        }

        JComboBox<AlbumFormatNormalizer.AudioFormat> formatPicker =
                new JComboBox<>(AlbumFormatNormalizer.AudioFormat.values());
        formatPicker.setSelectedItem(
                AlbumFormatNormalizer.fromSetting(Settings.AUDIO_TARGET_FORMAT.get())
        );

        Object[] message = {
                "Target format:", formatPicker,
                "Songs:", scopePicker
        };
        int choice = MDialog.showConfirmDialog(
                MainWindow.INSTANCE,
                message,
                "Reformat Songs",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        AlbumFormatNormalizer.AudioFormat target =
                (AlbumFormatNormalizer.AudioFormat) formatPicker.getSelectedItem();
        if (target == null) {
            return;
        }

        List<Song> scope;
        if (selected.isEmpty() || scopePicker.getSelectedIndex() == 1) {
            scope = allSongs;
        } else {
            scope = selected;
        }

        new ReformatMusicDialog(MainWindow.INSTANCE, scope, target).startAndShow();

        reloadTrackTable();
        MainWindow.INSTANCE.getLibraryUI().refresh();
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setOpaque(false);

        artworkImage = album.getArtworkImage();
        artwork.setPreferredSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));
        artwork.setMinimumSize(artwork.getPreferredSize());
        artwork.setMaximumSize(artwork.getPreferredSize());
        artwork.setArtwork(artworkImage);

        JPanel artworkWrapper = new JPanel(new BorderLayout());
        artworkWrapper.setOpaque(false);
        artworkWrapper.add(artwork, BorderLayout.NORTH);
        header.add(artworkWrapper, BorderLayout.WEST);

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

        JButton editButton = new JButton("Edit Album Info");
        editButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        editButton.addActionListener((_) -> new EditAlbumDialog(MainWindow.INSTANCE, album).setVisible(true));

        meta.add(editButton);

        meta.add(Box.createVerticalStrut(4));

        JButton importButton = new JButton("Import Songs...");
        importButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        importButton.setToolTipText(
                "Move songs from elsewhere in your library, or add songs from files, onto this album"
        );
        importButton.addActionListener((_) -> new ImportSongsDialog(MainWindow.INSTANCE, album).setVisible(true));

        meta.add(importButton);

        meta.add(Box.createVerticalStrut(4));

        JButton reformatButton = new JButton("Reformat...");
        reformatButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        reformatButton.addActionListener(_ -> reformatSongs());

        meta.add(reformatButton);

        meta.add(Box.createVerticalStrut(4));

        JButton exportButton = new JButton("Export Album...");
        exportButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportButton.addActionListener((_) -> {
            SystemFileChooser fc = new SystemFileChooser();
            fc.setAcceptAllFileFilterUsed(false);
            fc.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("Zip Files", "zip"));
            if( fc.showSaveDialog( this ) == SystemFileChooser.APPROVE_OPTION ) {
                File file = fc.getSelectedFile();
                new ExportMusicProcessingDialog(MainWindow.INSTANCE, album.getSongs(),file).startAndShow();
            }
        });

        meta.add(exportButton);

        meta.add(Box.createVerticalStrut(4));

        JButton playButton = new JButton("Play");
        playButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        playButton.addActionListener((_) -> {
            MusicPlayer.getInstance().addToQueue(album.getSongs().toArray(Song[]::new));
            MusicPlayer.getInstance().start();
        });

        meta.add(playButton);

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

    private String artistLabel() {
        return album.getArtist().artist();
    }

    private JScrollPane scrollableContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(
                CONTENT_PADDING_V,
                CONTENT_PADDING_H,
                CONTENT_PADDING_V,
                CONTENT_PADDING_H
        ));

        JPanel albumHeader = header();
        albumHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        albumHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, albumHeader.getPreferredSize().height));
        content.add(albumHeader);

        content.add(Box.createVerticalStrut(20));
        content.add(trackTables());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        return scrollPane;
    }

    private JPanel trackTables() {
        discTables.clear();

        List<Disc> discs = album.getDiscs();
        boolean labelled = discs.size() > 1;

        JPanel tables = new JPanel();
        tables.setOpaque(false);
        tables.setLayout(new BoxLayout(tables, BoxLayout.Y_AXIS));

        for (Disc disc : discs) {
            if (labelled) {
                JLabel discLabel = new JLabel(discLabel(disc, discs.size()));
                discLabel.setFont(discLabel.getFont().deriveFont(Font.BOLD, 14f));
                discLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                discLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                tables.add(discLabel);
                tables.add(Box.createVerticalStrut(6));
            }

            tables.add(createDiscTable(disc));

            if (labelled) {
                tables.add(Box.createVerticalStrut(16));
            }
        }

        return tables;
    }

    private static String discLabel(Disc disc, int discCount) {
        int total = Math.max(discCount, disc.getDiscTotal());
        return total > 1
                ? "Disc " + disc.getDiscIndex() + " of " + total
                : "Disc " + disc.getDiscIndex();
    }

    private JComponent createDiscTable(Disc disc) {
        List<Song> songs = disc.getSongs();
        String[] columns = {"", "#", "Title", "Artist", "Time"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // The checkbox that selects the track, and the track number.
                return column == 0 || column == 1;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : super.getColumnClass(columnIndex);
            }
        };
        fillModel(model, songs);

        JTable table = new JTable(model);
        table.getColumnModel()
                .getColumn(1)
                .setCellEditor(new DefaultCellEditor(newTrackNumberField()));

        DiscTable discTable = new DiscTable(disc, songs, model, table);
        configureTable(discTable);

        model.addTableModelListener(e -> onTrackNumberChanged(discTable, e));
        model.addTableModelListener(e -> {
            if (e.getColumn() == 0) {
                onSelectionChanged();
            }
        });

        discTables.add(discTable);

        return createTablePanel(table, songs);
    }

    private static @NonNull JComponent createTablePanel(JTable table, List<Song> songs) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(table.getTableHeader(), BorderLayout.NORTH);
        panel.add(table, BorderLayout.CENTER);

        int height = table.getTableHeader().getPreferredSize().height
                + table.getRowHeight() * Math.max(songs.size(), 1)
                + 4;
        panel.setPreferredSize(new Dimension(0, height));
        panel.setMinimumSize(new Dimension(0, height));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JFormattedTextField newTrackNumberField() {
        NumberFormatter formatter = new NumberFormatter(
                NumberFormat.getIntegerInstance()
        );
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(true);
        formatter.setCommitsOnValidEdit(false);
        return new JFormattedTextField(formatter);
    }

    private void configureTable(DiscTable discTable) {
        JTable table = discTable.table();

        table.setShowVerticalLines(false);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        table.getColumnModel().getColumn(0).setMaxWidth(32);
        table.getColumnModel().getColumn(1).setPreferredWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(320);
        table.getColumnModel().getColumn(3).setPreferredWidth(180);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(right);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    Song song = songAt(discTable, e.getPoint());
                    if (song != null) {
                        new EditSongDialog(MainWindow.INSTANCE, song).setVisible(true);
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
                handleRightClickMenu(e, songAt(discTable, e.getPoint()));
            }
        });
    }

    private static Song songAt(DiscTable discTable, Point point) {
        JTable table = discTable.table();
        List<Song> songs = discTable.songs();
        int row = table.rowAtPoint(point);
        return row >= 0 && row < songs.size() ? songs.get(row) : null;
    }

    private void onTrackNumberChanged(DiscTable discTable, TableModelEvent e) {
        if (updating.get() || bulkSelecting.get()) return;
        if (e.getType() != TableModelEvent.UPDATE || e.getColumn() != 1) return;

        DefaultTableModel model = discTable.model();
        List<Song> songs = discTable.songs();

        updating.set(true);
        try {
            int firstRow = Math.max(e.getFirstRow(), 0);
            int lastRow = Math.min(e.getLastRow(), model.getRowCount() - 1);

            ArrayList<ProcessTagsDialog.SongTagUpdate> records = new ArrayList<>();

            for (int row = firstRow; row <= lastRow; row++) {
                Object value = model.getValueAt(row, 1);
                int trackNumber = cellTrackNumber(value);

                if (trackNumber <= 0) {
                    if (MDialog.showConfirmDialog(
                            MainWindow.INSTANCE,
                            "The value you inputted is "+value+" and can break song indexing.\nDo you want to continue?",
                            "Empty value",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    ) == JOptionPane.NO_OPTION) {
                        trackNumber = songs.get(row).getTrackNumber();
                        model.setValueAt(String.valueOf(trackNumber), row, 1);
                    }
                }

                int trackTotal = Math.max(album.getTrackTotal(), trackNumber);

                records.add(new ProcessTagsDialog.SongTagUpdate(
                        songs.get(row),
                        Map.of(
                                FieldKey.TRACK,
                                String.valueOf(trackNumber),
                                FieldKey.TRACK_TOTAL,
                                String.valueOf(trackTotal)
                        ),
                        Set.of(),
                        album.isCompilation(),
                        ProcessTagsDialog.ArtworkAction.KEEP,
                        songs.get(row).getArtworkData(),
                        null
                ));
            }

            if (!records.isEmpty()) {
                new ProcessTagsDialog(MainWindow.INSTANCE, records).startAndShow();
            }
        } finally {
            updating.set(false);
        }
    }

    private static int cellTrackNumber(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void onSelectionChanged() {
        if (bulkSelecting.get()) return;
        rebuildSelection();
    }

    private void rebuildSelection() {
        selectedSongs.clear();
        for (DiscTable discTable : discTables) {
            DefaultTableModel model = discTable.model();
            List<Song> songs = discTable.songs();
            for (int row = 0; row < model.getRowCount() && row < songs.size(); row++) {
                if (Boolean.TRUE.equals(model.getValueAt(row, 0))) {
                    selectedSongs.add(songs.get(row));
                }
            }
        }
        if (selectionListener != null) {
            selectionListener.accept(getSelectedSongs());
        }
    }

    private record DiscTable(Disc disc, List<Song> songs, DefaultTableModel model, JTable table) {
    }

    public void reloadTrackTable() {
        if (discTables.size() != album.getDiscs().size()) {
            refresh();
            return;
        }

        bulkSelecting.set(true);
        try {
            for (DiscTable discTable : discTables) {
                fillModel(discTable.model(), discTable.songs());
            }
        } finally {
            bulkSelecting.set(false);
        }
        rebuildSelection();
    }

    private static void fillModel(DefaultTableModel model, List<Song> songs) {
        model.setRowCount(0);
        for (Song song : songs) {
            String number = song.getTrackNumber() > 0
                    ? String.valueOf(song.getTrackNumber())
                    : "";
            model.addRow(new Object[]{
                    false,
                    number,
                    song.getTitle(),
                    song.getArtist(),
                    trackLength(song)
            });
        }
    }

    private static String trackLength(Song song) {
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
                String probe = current.isEmpty()
                        ? word
                        : current + " " + word;
                if (fm.stringWidth(probe) > available && !current.isEmpty()) {
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
            JMenuItem playSong = new JMenuItem("Play");
            playSong.addActionListener(_ -> {
                MusicPlayer.getInstance().addToQueue(songsToEdit.toArray(Song[]::new));
                MusicPlayer.getInstance().start();
            });
            popupMenu.add(playSong);

            String label = songsToEdit.size() > 1
                    ? "Get Info (" + songsToEdit.size() + " Songs)..."
                    : "Get Info...";
            JMenuItem editSong = new JMenuItem(label);
            editSong.addActionListener(_ -> new EditSongDialog(MainWindow.INSTANCE, songsToEdit).setVisible(true));
            popupMenu.add(editSong);
            popupMenu.addSeparator();
        }

        JMenuItem editAlbum = new JMenuItem("Edit Album Info...");
        editAlbum.addActionListener(_ -> new EditAlbumDialog(MainWindow.INSTANCE, album).setVisible(true));
        popupMenu.add(editAlbum);

        popupMenu.addSeparator();

        JMenuItem selectAll = new JMenuItem("Select All");
        selectAll.addActionListener(_ -> selectAllTracks());
        popupMenu.add(selectAll);

        JMenuItem deselectAll = new JMenuItem("Deselect All");
        deselectAll.addActionListener(_ -> deselectAllTracks());
        popupMenu.add(deselectAll);

        if (!songsToEdit.isEmpty()) {
            popupMenu.addSeparator();

            JMenuItem delete = createDelete(songsToEdit);
            popupMenu.add(delete);
        }

        if (mouseEvent.getComponent() instanceof JTable table) {
            popupMenu.show(table, mouseEvent.getX(), mouseEvent.getY());
        } else {
            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }
    }

    private @NonNull JMenuItem createDelete(List<Song> songsToEdit) {
        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(_ -> {
            if (
                    MDialog.showOptionDialog(
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
            }
        });
        return delete;
    }
}
