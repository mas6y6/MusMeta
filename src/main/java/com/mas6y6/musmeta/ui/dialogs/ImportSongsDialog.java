package com.mas6y6.musmeta.ui.dialogs;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.AlbumTags;
import com.mas6y6.musmeta.core.Duplicates;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.dialogs.base.MDialog;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportSongsDialog extends JDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportSongsDialog.class);

    private static final Dimension DIALOG_SIZE = new Dimension(760, 520);

    private record Candidate(Song song, boolean fromLibrary) {
    }

    private final Album album;
    private final Window owner;

    private final List<Candidate> candidates = new ArrayList<>();
    private final Set<String> knownPaths = new HashSet<>();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"", "Title", "Artist", "Album", "File"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }
    };

    private final JTable table = new JTable(model);
    private final JLabel summary = new JLabel();
    private final JButton addFilesButton = new JButton("Add Files...");
    private final JButton removeButton = new JButton("Remove");
    private final JButton importButton = new JButton("Import");

    public ImportSongsDialog(Window owner, Album album) {
        super(owner, "Import Songs", ModalityType.APPLICATION_MODAL);
        this.owner = owner != null ? owner : this;
        this.album = album;

        setSize(DIALOG_SIZE);
        setMinimumSize(new Dimension(620, 420));
        setLocationRelativeTo(this.owner);

        candidates.addAll(libraryCandidates());
        for (Candidate candidate : candidates) {
            addRow(candidate, false);
        }
        updateSummary();

        setContentPane(content());
    }

    private List<Candidate> libraryCandidates() {
        Set<Song> own = identitySet(album.getSongs());
        Set<String> paths = new HashSet<>();

        List<Song> songs = new ArrayList<>();
        for (Song song : Library.getInstance().getSongs()) {
            if (own.contains(song)) {
                continue;
            }
            songs.add(song);
            paths.add(pathOf(song));
        }

        songs.sort(Comparator
                .comparing(Song::getAlbum, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(Song::getDiscNumber)
                .thenComparingInt(Song::getTrackNumber)
                .thenComparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER));

        knownPaths.addAll(paths);

        List<Candidate> result = new ArrayList<>();
        for (Song song : songs) {
            result.add(new Candidate(song, true));
        }
        return result;
    }

    //region Layout

    private JComponent content() {
        JPanel page = new JPanel(new BorderLayout(10, 12));
        page.setBorder(new EmptyBorder(20, 25, 20, 25));
        page.add(header(), BorderLayout.NORTH);
        page.add(listPanel(), BorderLayout.CENTER);
        page.add(footer(), BorderLayout.SOUTH);
        return page;
    }

    private JComponent header() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Import Songs into \"" + album.getTitle() + "\"");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);

        header.add(Box.createVerticalStrut(8));

        JLabel description = new JLabel(
                "<html><body>Pick songs from elsewhere in your library, or add songs from files."
                        + "<br>The album's own album, album artist and compilation tags are copied onto them."
                        + "</body></html>"
        );
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(description);

        return header;
    }

    private JComponent listPanel() {
        table.setRowHeight(26);
        table.setShowVerticalLines(false);
        table.getColumnModel().getColumn(0).setMaxWidth(32);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);
        table.getColumnModel().getColumn(4).setPreferredWidth(180);

        model.addTableModelListener(e -> {
            if (e.getColumn() == 0) {
                updateSummary();
            }
        });

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(listButtons(), BorderLayout.SOUTH);
        return panel;
    }

    private JComponent listButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);

        addFilesButton.addActionListener(e -> chooseFiles());
        panel.add(addFilesButton);

        panel.add(Box.createHorizontalStrut(12));

        JButton selectAll = new JButton("Select All");
        selectAll.addActionListener(e -> setAllChecked(true));
        panel.add(selectAll);

        JButton selectNone = new JButton("Select None");
        selectNone.addActionListener(e -> setAllChecked(false));
        panel.add(selectNone);

        removeButton.setToolTipText("Take songs that were just added from files back off the list");
        removeButton.addActionListener(e -> removeAddedFiles());
        panel.add(removeButton);

        return panel;
    }

    private JComponent footer() {
        summary.setFont(summary.getFont().deriveFont(Font.PLAIN, 11f));
        summary.setForeground(UIManager.getColor("Label.disabledForeground"));

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        importButton.addActionListener(e -> importSelected());
        getRootPane().setDefaultButton(importButton);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(importButton);

        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.add(summary, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    //endregion

    //region Candidate list

    private void addRow(Candidate candidate, boolean checked) {
        Song song = candidate.song();
        File file = song.getSourceAudioFile();
        model.addRow(new Object[]{
                checked,
                song.getTitle(),
                song.getArtist(),
                song.getAlbum(),
                file != null ? file.getName() : ""
        });
    }

    private void setAllChecked(boolean checked) {
        for (int row = 0; row < model.getRowCount(); row++) {
            model.setValueAt(checked, row, 0);
        }
    }

    private List<Candidate> selectedCandidates() {
        List<Candidate> selected = new ArrayList<>();
        for (int row = 0; row < model.getRowCount() && row < candidates.size(); row++) {
            if (Boolean.TRUE.equals(model.getValueAt(row, 0))) {
                selected.add(candidates.get(row));
            }
        }
        return selected;
    }

    private void removeAddedFiles() {
        for (int row = model.getRowCount() - 1; row >= 0; row--) {
            if (Boolean.TRUE.equals(model.getValueAt(row, 0)) && !candidates.get(row).fromLibrary()) {
                knownPaths.remove(pathOf(candidates.get(row).song()));
                candidates.remove(row);
                model.removeRow(row);
            }
        }
        updateSummary();
    }

    private void updateSummary() {
        List<Candidate> selected = selectedCandidates();

        boolean removable = selected.stream().anyMatch(candidate -> !candidate.fromLibrary());
        removeButton.setEnabled(removable);

        if (selected.isEmpty()) {
            summary.setText(candidates.isEmpty()
                    ? "No songs available to import."
                    : "No songs selected.");
            importButton.setEnabled(false);
            return;
        }

        long clashing = selected.stream()
                .filter(candidate -> !AlbumTags.negotiableConflicts(List.of(candidate.song()), album).isEmpty())
                .count();

        int count = selected.size();
        String text = count == 1 ? "1 song selected." : count + " songs selected.";
        if (clashing > 0) {
            text += " " + clashing + " of them state album tags that differ from this album.";
        }
        summary.setText(text);
        importButton.setEnabled(true);
    }

    //endregion

    //region Adding songs from files

    private void chooseFiles() {
        var chooser = new SystemFileChooser(System.getProperty("user.home"));
        chooser.setDialogTitle("Select Songs to Import");
        chooser.setMultiSelectionEnabled(true);
        chooser.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter(
                "Audio Files",
                Constants.MUSIC_EXTENSIONS.toArray(String[]::new)
        ));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] files = chooser.getSelectedFiles();
        if (files == null || files.length == 0) {
            File single = chooser.getSelectedFile();
            files = single != null ? new File[]{single} : new File[0];
        }
        if (files.length > 0) {
            readFiles(files);
        }
    }

    /**
     * Reads the chosen files off the event thread, since reading the tags and
     * audio headers of a large selection takes long enough to be noticeable.
     */
    private void readFiles(File[] files) {
        addFilesButton.setEnabled(false);
        summary.setText("Reading " + files.length + (files.length == 1 ? " file..." : " files..."));

        new SwingWorker<List<Candidate>, Void>() {
            @Override
            protected List<Candidate> doInBackground() {
                List<Candidate> read = new ArrayList<>();
                for (File file : files) {
                    try {
                        Song song = new Song(AudioFileIO.read(file));
                        // A file already on the list, whether it was picked
                        // earlier in this dialog or is already in the library,
                        // is not offered a second time.
                        if (knownPaths.add(pathOf(song))) {
                            read.add(new Candidate(song, false));
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Skipping unreadable audio file: {}", file, e);
                    }
                }
                return read;
            }

            @Override
            protected void done() {
                addFilesButton.setEnabled(true);
                try {
                    for (Candidate candidate : get()) {
                        candidates.add(candidate);
                        addRow(candidate, true);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to read the selected files", e);
                }
                updateSummary();
            }
        }.execute();
    }

    //endregion

    //region Importing

    private void importSelected() {
        List<Candidate> selected = selectedCandidates();
        if (selected.isEmpty()) {
            return;
        }

        List<Song> fromLibrary = new ArrayList<>();
        List<Song> fromFiles = new ArrayList<>();
        for (Candidate candidate : selected) {
            if (candidate.fromLibrary()) {
                fromLibrary.add(candidate.song());
            } else {
                fromFiles.add(candidate.song());
            }
        }

        List<Song> newFiles = resolveDuplicates(fromFiles);

        List<Song> toImport = new ArrayList<>(fromLibrary);
        toImport.addAll(newFiles);

        if (toImport.isEmpty()) {
            MDialog.showMessageDialog(
                    this,
                    "None of the selected songs will be imported.",
                    "Nothing to Import",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Set<AlbumTags.Essential> conflicts = AlbumTags.negotiableConflicts(toImport, album);
        Map<AlbumTags.Essential, ImportTagConflictDialog.Choice> decisions = conflicts.isEmpty()
                ? Map.of()
                : ImportTagConflictDialog.showAndResolve(owner, toImport, album, conflicts);
        if (decisions == null) {
            return;
        }

        List<ProcessTagsDialog.SongTagUpdate> updates = buildUpdates(toImport, decisions);

        dispose();

        // Songs that came off disk are brought into the library's audio format
        // before any tag is written, so the album tags land on the converted
        // file that is kept rather than on the original that is left behind.
        // Songs already in the library were normalised when they were first
        // imported and are only moved between albums here, so they are skipped.
        convertIfNeeded(newFiles);

        var processDialog = new ProcessTagsDialog(owner, updates);
        processDialog.setOnComplete(() -> openSongEditor(toImport));
        processDialog.startAndShow();
    }

    /**
     * Converts the given songs into the configured audio format when at least
     * one of them is stored in another one. The import carries on either way:
     * a song that could not be converted is still perfectly importable, it
     * simply keeps the format it came in.
     */
    private void convertIfNeeded(List<Song> songs) {
        AlbumFormatNormalizer.AudioFormat target =
                AlbumFormatNormalizer.fromSetting(Settings.AUDIO_TARGET_FORMAT.get());

        if (ConvertImportedSongsDialog.isNeeded(songs, target)) {
            new ConvertImportedSongsDialog(owner, songs).startAndShow();
        }
    }

    /**
     * Drops the newly added files the user chose not to import and clears out
     * the library entries a chosen file replaces, the same way a plain import
     * from the File menu does.
     *
     * @return the files that should actually be imported
     */
    private List<Song> resolveDuplicates(List<Song> incoming) {
        if (incoming.isEmpty()) {
            return List.of();
        }

        Library library = Library.getInstance();
        List<Duplicates.Group> groups = Duplicates.findGroups(incoming, library.getSongs());
        if (groups.isEmpty()) {
            return incoming;
        }

        Map<Duplicates.Group, Song> choices = DuplicateImportDialog.resolve(owner, groups);
        if (choices == null) {
            return List.of();
        }

        Map<Song, Duplicates.Group> groupOf = new IdentityHashMap<>();
        for (Duplicates.Group group : groups) {
            for (Song song : group.incoming()) {
                groupOf.put(song, group);
            }
        }

        Set<Duplicates.Group> handled = identitySet(List.of());
        List<Song> toImport = new ArrayList<>();
        for (Song song : incoming) {
            Duplicates.Group group = groupOf.get(song);
            if (group == null) {
                toImport.add(song);
                continue;
            }
            if (!handled.add(group)) {
                continue;
            }
            Song chosen = choices.get(group);
            if (chosen != null && group.incoming().contains(chosen)) {
                group.existing().forEach(library::removeSong);
                toImport.add(chosen);
            }
        }
        return toImport;
    }

    /**
     * Builds the tag writes that move the given songs onto this album.
     *
     * <p>The album title is always written, since that is the value the library
     * files a song under. The album artist and the compilation flag are only
     * written for the tags the user resolved in favour of the album, so a song
     * that was told to keep its own value keeps it. The disc and track totals
     * always follow the album, since they describe its shape rather than the
     * song.
     *
     * @param decisions the answer given per album tag
     */
    private List<ProcessTagsDialog.SongTagUpdate> buildUpdates(
            List<Song> songs,
            Map<AlbumTags.Essential, ImportTagConflictDialog.Choice> decisions
    ) {
        String title = album.getTitle();
        int discTotal = album.getDiscTotal();
        int trackTotal = Math.max(album.getTrackTotal(), album.getSongs().size() + songs.size());

        boolean useAlbumArtist = useAlbumValue(decisions, AlbumTags.Essential.ALBUM_ARTIST);
        String albumArtist = AlbumTags.Essential.ALBUM_ARTIST.albumValue(album);
        Boolean compilation = useAlbumValue(decisions, AlbumTags.Essential.COMPILATION)
                ? album.isCompilation()
                : null;

        List<ProcessTagsDialog.SongTagUpdate> updates = new ArrayList<>();
        for (Song song : songs) {
            Map<FieldKey, String> toSet = new HashMap<>();
            Set<FieldKey> toDelete = new HashSet<>();

            toSet.put(FieldKey.ALBUM, title);

            if (useAlbumArtist) {
                if (albumArtist.isBlank()) {
                    toDelete.add(FieldKey.ALBUM_ARTIST);
                } else {
                    toSet.put(FieldKey.ALBUM_ARTIST, albumArtist);
                }
            }

            toSet.put(FieldKey.DISC_NO, String.valueOf(Math.min(Math.max(1, song.getDiscNumber()), discTotal)));
            toSet.put(FieldKey.DISC_TOTAL, String.valueOf(discTotal));
            toSet.put(FieldKey.TRACK_TOTAL, String.valueOf(Math.max(trackTotal, song.getTrackTotal())));

            updates.add(new ProcessTagsDialog.SongTagUpdate(
                    song,
                    toSet,
                    toDelete,
                    compilation,
                    ProcessTagsDialog.ArtworkAction.KEEP,
                    null,
                    null
            ));
        }

        return updates;
    }

    private static boolean useAlbumValue(
            Map<AlbumTags.Essential, ImportTagConflictDialog.Choice> decisions,
            AlbumTags.Essential tag
    ) {
        return decisions.getOrDefault(tag, ImportTagConflictDialog.Choice.USE_ALBUM_VALUE)
                == ImportTagConflictDialog.Choice.USE_ALBUM_VALUE;
    }

    private void openSongEditor(List<Song> songs) {
        if (songs.size() == 1) {
            new EditSongDialog(owner, songs.get(0)).setVisible(true);
        } else {
            new EditSongDialog(owner, songs).setVisible(true);
        }
    }

    //endregion

    private static <T> Set<T> identitySet(List<T> items) {
        Set<T> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(items);
        return set;
    }

    private static String pathOf(Song song) {
        File file = song.getSourceAudioFile();
        return file == null ? "" : file.toPath().toAbsolutePath().normalize().toString();
    }
}
