package com.mas6y6.musmeta.ui.dialogs;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.ui.album.AlbumUI;
import com.mas6y6.musmeta.ui.components.album.AlbumArtwork;
import org.jaudiotagger.tag.FieldKey;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class EditSongDialog extends JDialog {
    private static final Dimension DIALOG_SIZE = new Dimension(880, 540);
    private static final int ARTWORK_SIZE = 180;

    private final List<Song> songs;
    private final boolean isSingleSong;

    // Single Song Fields
    private JTextField titleField;
    private JSpinner trackNoSpinner;
    private JSpinner trackTotalSpinner;
    private JSpinner discNoSpinner;
    private JSpinner discTotalSpinner;

    // Common / Shared Fields
    private JTextField artistField;
    private JTextField albumField;
    private JTextField albumArtistField;
    private JComboBox<String> genreCombo;
    private JTextField yearField;
    private JTextField composerField;
    private JTextField groupingField;
    private JComboBox<String> ratingCombo;
    private JTextField bpmField;
    private JCheckBox compilationCheck;
    private JTextField commentField;

    // Multi-select enabled checkboxes
    private final Map<String, JCheckBox> applyCheckboxes = new HashMap<>();

    // Artwork
    private AlbumArtwork artworkLabel;
    private ProcessTagsDialog.ArtworkAction artworkAction = ProcessTagsDialog.ArtworkAction.KEEP;
    private byte[] newArtworkBytes;
    private String newArtworkMimeType;

    public EditSongDialog(Window parent, Song song) {
        this(parent, List.of(song));
    }

    public EditSongDialog(Window parent, List<Song> songs) {
        super(parent, songs.size() == 1 ? "Song Info" : "Multiple Items Info (" + songs.size() + " songs)", ModalityType.APPLICATION_MODAL);
        this.songs = songs != null && !songs.isEmpty() ? songs : List.of();
        this.isSingleSong = this.songs.size() == 1;

        setSize(DIALOG_SIZE);
        setMinimumSize(new Dimension(620, 480));
        setLocationRelativeTo(parent);

        initFields();
        initComponents();
    }

    private void initFields() {
        if (isSingleSong) {
            Song song = songs.get(0);
            titleField = new JTextField(song.getTitle());
            artistField = new JTextField(song.getRawArtist().isBlank() ? song.getArtist() : song.getRawArtist());
            albumField = new JTextField(song.getAlbum());
            albumArtistField = new JTextField(song.getRawAlbumArtist());

            trackNoSpinner = new JSpinner(new SpinnerNumberModel(Math.max(0, song.getTrackNumber()), 0, 999, 1));
            trackTotalSpinner = new JSpinner(new SpinnerNumberModel(Math.max(0, song.getTrackTotal()), 0, 999, 1));
            discNoSpinner = new JSpinner(new SpinnerNumberModel(Math.max(1, song.getDiscNumber()), 1, 99, 1));
            discTotalSpinner = new JSpinner(new SpinnerNumberModel(Math.max(1, song.getDiscTotal()), 1, 99, 1));

            genreCombo = new JComboBox<>(EditAlbumDialog.GENRES);
            genreCombo.setEditable(true);
            genreCombo.setSelectedItem(song.getGenre());

            yearField = new JTextField(song.getYear());
            composerField = new JTextField(song.getComposer());
            groupingField = new JTextField(song.getGrouping());

            ratingCombo = new JComboBox<>(EditAlbumDialog.RATINGS);
            ratingCombo.setSelectedItem(ratingToLabel(song.getRating()));

            bpmField = new JTextField(song.getBpm());
            compilationCheck = new JCheckBox("Part of a compilation", song.isCompilation());
            commentField = new JTextField(song.getComment());
        } else {
            // Multi song mode
            String commonArtist = getCommonValue(songs, Song::getRawArtist);
            artistField = new JTextField(commonArtist);

            String commonAlbum = getCommonValue(songs, Song::getAlbum);
            albumField = new JTextField(commonAlbum);

            String commonAlbumArtist = getCommonValue(songs, Song::getRawAlbumArtist);
            albumArtistField = new JTextField(commonAlbumArtist);

            genreCombo = new JComboBox<>(EditAlbumDialog.GENRES);
            genreCombo.setEditable(true);
            String commonGenre = getCommonValue(songs, Song::getGenre);
            genreCombo.setSelectedItem(commonGenre);

            yearField = new JTextField(getCommonValue(songs, Song::getYear));
            composerField = new JTextField(getCommonValue(songs, Song::getComposer));
            groupingField = new JTextField(getCommonValue(songs, Song::getGrouping));

            ratingCombo = new JComboBox<>(EditAlbumDialog.RATINGS);
            ratingCombo.setSelectedItem(ratingToLabel(getCommonValue(songs, Song::getRating)));

            bpmField = new JTextField(getCommonValue(songs, Song::getBpm));

            boolean allCompilation = songs.stream().allMatch(Song::isCompilation);
            compilationCheck = new JCheckBox("Part of a compilation", allCompilation);

            discTotalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
            trackTotalSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));

            commentField = new JTextField(getCommonValue(songs, Song::getComment));
        }
    }

    private static String getCommonValue(List<Song> songs, java.util.function.Function<Song, String> extractor) {
        if (songs.isEmpty()) return "";
        String first = extractor.apply(songs.get(0));
        for (Song s : songs) {
            if (!Objects.equals(first, extractor.apply(s))) {
                return "";
            }
        }
        return first != null ? first : "";
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(16, 12));
        contentPane.setBorder(new EmptyBorder(16, 20, 16, 20));

        // West: Artwork
        contentPane.add(createArtworkPanel(), BorderLayout.WEST);

        // Center: Form
        JScrollPane formScroll = new JScrollPane(createFormPanel());
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        contentPane.add(formScroll, BorderLayout.CENTER);

        // South: Buttons
        contentPane.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(contentPane);
    }

    private JPanel createArtworkPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        artworkLabel = new AlbumArtwork(10);
        artworkLabel.setPreferredSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));
        artworkLabel.setMaximumSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));
        artworkLabel.setMinimumSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));

        Image currentImage = null;
        if (isSingleSong && !songs.isEmpty()) {
            currentImage = songs.get(0).getArtworkImage();
        }
        artworkLabel.setArtwork(currentImage != null ? currentImage : placeholderArtwork());
        artworkLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        artworkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        artworkLabel.setToolTipText("Click or drag an image here to change artwork");

        artworkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    chooseArtworkFile();
                }
            }
        });

        artworkLabel.setDropTarget(new DropTarget(artworkLabel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Object data = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (data instanceof List<?> list && !list.isEmpty()) {
                        Object first = list.get(0);
                        if (first instanceof File file) {
                            applyArtworkFile(file);
                        }
                    }
                } catch (Exception ex) {
                    // Ignore invalid dropped data
                }
            }
        }));

        panel.add(artworkLabel);
        panel.add(Box.createVerticalStrut(10));

        JButton chooseButton = new JButton("Choose Artwork...");
        chooseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        chooseButton.setMaximumSize(new Dimension(ARTWORK_SIZE, 30));
        chooseButton.addActionListener(e -> chooseArtworkFile());
        panel.add(chooseButton);

        panel.add(Box.createVerticalStrut(6));

        JButton removeButton = new JButton("Remove Artwork");
        removeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeButton.setMaximumSize(new Dimension(ARTWORK_SIZE, 30));
        removeButton.addActionListener(e -> {
            artworkAction = ProcessTagsDialog.ArtworkAction.REMOVE;
            newArtworkBytes = null;
            newArtworkMimeType = null;
            artworkLabel.setArtwork(placeholderArtwork());
        });
        panel.add(removeButton);

        return panel;
    }

    private void chooseArtworkFile() {
        var fsc = new SystemFileChooser(System.getProperty("user.home"));
        fsc.setDialogTitle("Select Artwork Image");
        fsc.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("Image Files (*.jpg, *.jpeg, *.png, *.webp, *.bmp)", "jpg", "jpeg", "png", "webp", "bmp"));
        fsc.setAcceptAllFileFilterUsed(false);
        if (fsc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = fsc.getSelectedFile();
            if (selected != null && selected.isFile()) {
                applyArtworkFile(selected);
            }
        }
    }

    private void applyArtworkFile(File file) {
        try {
            Image img = ImageIO.read(file);
            if (img != null) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String ext = com.google.common.io.Files.getFileExtension(file.getName()).toLowerCase();
                String mime = "image/png".equals(ext) ? "image/png" : "image/jpeg";
                newArtworkBytes = bytes;
                newArtworkMimeType = mime;
                artworkAction = ProcessTagsDialog.ArtworkAction.REPLACE;
                artworkLabel.setArtwork(img);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not load image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        if (isSingleSong) {
            addFormRow(panel, gbc, row++, "Title:", titleField, null);
            addFormRow(panel, gbc, row++, "Artist:", artistField, null);
            addFormRow(panel, gbc, row++, "Album:", albumField, null);
            addFormRow(panel, gbc, row++, "Album Artist:", albumArtistField, null);

            // Track & Disc numbers
            JPanel trackPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            trackPanel.setOpaque(false);
            trackPanel.add(new JLabel("Track:"));
            trackPanel.add(trackNoSpinner);
            trackPanel.add(new JLabel("of"));
            trackPanel.add(trackTotalSpinner);

            trackPanel.add(Box.createHorizontalStrut(10));
            trackPanel.add(new JLabel("Disc:"));
            trackPanel.add(discNoSpinner);
            trackPanel.add(new JLabel("of"));
            trackPanel.add(discTotalSpinner);
            addFormRow(panel, gbc, row++, "Track / Disc:", trackPanel, null);

            // Compilation toggle
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(compilationCheck, gbc);
            gbc.gridwidth = 1;

            addFormRow(panel, gbc, row++, "Genre:", genreCombo, null);
            addFormRow(panel, gbc, row++, "Year:", yearField, null);
            addFormRow(panel, gbc, row++, "Composer:", composerField, null);
            addFormRow(panel, gbc, row++, "Grouping:", groupingField, null);
            addFormRow(panel, gbc, row++, "Rating:", ratingCombo, null);
            addFormRow(panel, gbc, row++, "BPM:", bpmField, null);
            addFormRow(panel, gbc, row++, "Comments:", commentField, null);
        } else {
            // Multi-item form with enable checkboxes next to fields
            addFormRow(panel, gbc, row++, "Artist:", artistField, "artist");
            addFormRow(panel, gbc, row++, "Album:", albumField, "album");
            addFormRow(panel, gbc, row++, "Album Artist:", albumArtistField, "albumArtist");
            addFormRow(panel, gbc, row++, "Genre:", genreCombo, "genre");
            addFormRow(panel, gbc, row++, "Year:", yearField, "year");
            addFormRow(panel, gbc, row++, "Composer:", composerField, "composer");
            addFormRow(panel, gbc, row++, "Grouping:", groupingField, "grouping");
            addFormRow(panel, gbc, row++, "Rating:", ratingCombo, "rating");
            addFormRow(panel, gbc, row++, "BPM:", bpmField, "bpm");

            // Totals
            JPanel totalsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            totalsPanel.setOpaque(false);
            totalsPanel.add(new JLabel("Total Discs:"));
            totalsPanel.add(discTotalSpinner);
            totalsPanel.add(Box.createHorizontalStrut(10));
            totalsPanel.add(new JLabel("Total Tracks:"));
            totalsPanel.add(trackTotalSpinner);
            addFormRow(panel, gbc, row++, "Track/Disc Totals:", totalsPanel, "totals");

            // Compilation toggle
            JCheckBox applyCompCheck = new JCheckBox();
            applyCheckboxes.put("compilation", applyCompCheck);
            JPanel compPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            compPanel.setOpaque(false);
            compPanel.add(applyCompCheck);
            compPanel.add(compilationCheck);

            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(compPanel, gbc);
            gbc.gridwidth = 1;

            addFormRow(panel, gbc, row++, "Comments:", commentField, "comment");
        }

        return panel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, Component comp, String multiKey) {
        if (!isSingleSong && multiKey != null) {
            JCheckBox applyCheck = new JCheckBox();
            applyCheckboxes.put(multiKey, applyCheck);

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            labelPanel.setOpaque(false);
            labelPanel.add(applyCheck);
            labelPanel.add(new JLabel(labelText));

            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.EAST;
            panel.add(labelPanel, gbc);

            // Auto-check apply box when user changes the component
            if (comp instanceof JTextField tf) {
                tf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) { applyCheck.setSelected(true); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e) { applyCheck.setSelected(true); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { applyCheck.setSelected(true); }
                });
            } else if (comp instanceof JComboBox<?> cb) {
                cb.addActionListener(e -> applyCheck.setSelected(true));
            }
        } else {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.EAST;
            panel.add(new JLabel(labelText), gbc);
        }

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(comp, gbc);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> onSave());
        getRootPane().setDefaultButton(okButton);

        panel.add(cancelButton);
        panel.add(okButton);

        return panel;
    }

    private void onSave() {
        if (songs.isEmpty()) {
            dispose();
            return;
        }

        List<ProcessTagsDialog.SongTagUpdate> updates = new ArrayList<>();

        if (isSingleSong) {
            Song song = songs.get(0);
            Map<FieldKey, String> toSet = new HashMap<>();
            Set<FieldKey> toDelete = new HashSet<>();

            String title = titleField.getText().trim();
            if (!title.isBlank()) {
                toSet.put(FieldKey.TITLE, title);
            }

            String artist = artistField.getText().trim();
            if (!artist.isBlank()) {
                toSet.put(FieldKey.ARTIST, artist);
            } else {
                toDelete.add(FieldKey.ARTIST);
            }

            String album = albumField.getText().trim();
            if (!album.isBlank()) {
                toSet.put(FieldKey.ALBUM, album);
            }

            String albumArtist = albumArtistField.getText().trim();
            if (!albumArtist.isBlank()) {
                toSet.put(FieldKey.ALBUM_ARTIST, albumArtist);
            } else {
                toDelete.add(FieldKey.ALBUM_ARTIST);
            }

            int trackNo = (Integer) trackNoSpinner.getValue();
            if (trackNo > 0) {
                toSet.put(FieldKey.TRACK, String.valueOf(trackNo));
            } else {
                toDelete.add(FieldKey.TRACK);
            }

            int trackTotal = (Integer) trackTotalSpinner.getValue();
            if (trackTotal > 0) {
                toSet.put(FieldKey.TRACK_TOTAL, String.valueOf(trackTotal));
            } else {
                toDelete.add(FieldKey.TRACK_TOTAL);
            }

            int discNo = (Integer) discNoSpinner.getValue();
            if (discNo > 0) {
                toSet.put(FieldKey.DISC_NO, String.valueOf(discNo));
            }

            int discTotal = (Integer) discTotalSpinner.getValue();
            if (discTotal > 0) {
                toSet.put(FieldKey.DISC_TOTAL, String.valueOf(discTotal));
            }

            String genre = genreCombo.getSelectedItem() != null ? genreCombo.getSelectedItem().toString().trim() : "";
            if (!genre.isBlank()) {
                toSet.put(FieldKey.GENRE, genre);
            } else {
                toDelete.add(FieldKey.GENRE);
            }

            String year = yearField.getText().trim();
            if (!year.isBlank()) {
                toSet.put(FieldKey.YEAR, year);
            } else {
                toDelete.add(FieldKey.YEAR);
            }

            String composer = composerField.getText().trim();
            if (!composer.isBlank()) {
                toSet.put(FieldKey.COMPOSER, composer);
            } else {
                toDelete.add(FieldKey.COMPOSER);
            }

            String grouping = groupingField.getText().trim();
            if (!grouping.isBlank()) {
                toSet.put(FieldKey.GROUPING, grouping);
            } else {
                toDelete.add(FieldKey.GROUPING);
            }

            String rating = labelToRating(ratingCombo.getSelectedItem() != null ? ratingCombo.getSelectedItem().toString() : "");
            if (!rating.isBlank() && !"0".equals(rating)) {
                toSet.put(FieldKey.RATING, rating);
            } else {
                toDelete.add(FieldKey.RATING);
            }

            String bpm = bpmField.getText().trim();
            if (!bpm.isBlank()) {
                toSet.put(FieldKey.BPM, bpm);
            } else {
                toDelete.add(FieldKey.BPM);
            }

            String comment = commentField.getText().trim();
            if (!comment.isBlank()) {
                toSet.put(FieldKey.COMMENT, comment);
            } else {
                toDelete.add(FieldKey.COMMENT);
            }

            boolean compilation = compilationCheck.isSelected();

            updates.add(new ProcessTagsDialog.SongTagUpdate(
                    song,
                    toSet,
                    toDelete,
                    compilation,
                    artworkAction,
                    newArtworkBytes,
                    newArtworkMimeType
            ));
        } else {
            // Multi song mode: check apply boxes
            boolean applyArtist = isFieldChecked("artist");
            String artist = artistField.getText().trim();

            boolean applyAlbum = isFieldChecked("album");
            String album = albumField.getText().trim();

            boolean applyAlbumArtist = isFieldChecked("albumArtist");
            String albumArtist = albumArtistField.getText().trim();

            boolean applyGenre = isFieldChecked("genre");
            String genre = genreCombo.getSelectedItem() != null ? genreCombo.getSelectedItem().toString().trim() : "";

            boolean applyYear = isFieldChecked("year");
            String year = yearField.getText().trim();

            boolean applyComposer = isFieldChecked("composer");
            String composer = composerField.getText().trim();

            boolean applyGrouping = isFieldChecked("grouping");
            String grouping = groupingField.getText().trim();

            boolean applyRating = isFieldChecked("rating");
            String rating = labelToRating(ratingCombo.getSelectedItem() != null ? ratingCombo.getSelectedItem().toString() : "");

            boolean applyBpm = isFieldChecked("bpm");
            String bpm = bpmField.getText().trim();

            boolean applyTotals = isFieldChecked("totals");
            int discTotal = (Integer) discTotalSpinner.getValue();
            int trackTotal = (Integer) trackTotalSpinner.getValue();

            boolean applyCompilation = isFieldChecked("compilation");
            boolean compilation = compilationCheck.isSelected();

            boolean applyComment = isFieldChecked("comment");
            String comment = commentField.getText().trim();

            for (Song song : songs) {
                Map<FieldKey, String> toSet = new HashMap<>();
                Set<FieldKey> toDelete = new HashSet<>();

                if (applyArtist) {
                    if (!artist.isBlank()) toSet.put(FieldKey.ARTIST, artist);
                    else toDelete.add(FieldKey.ARTIST);
                }
                if (applyAlbum && !album.isBlank()) {
                    toSet.put(FieldKey.ALBUM, album);
                }
                if (applyAlbumArtist) {
                    if (!albumArtist.isBlank()) toSet.put(FieldKey.ALBUM_ARTIST, albumArtist);
                    else toDelete.add(FieldKey.ALBUM_ARTIST);
                }
                if (applyGenre) {
                    if (!genre.isBlank()) toSet.put(FieldKey.GENRE, genre);
                    else toDelete.add(FieldKey.GENRE);
                }
                if (applyYear) {
                    if (!year.isBlank()) toSet.put(FieldKey.YEAR, year);
                    else toDelete.add(FieldKey.YEAR);
                }
                if (applyComposer) {
                    if (!composer.isBlank()) toSet.put(FieldKey.COMPOSER, composer);
                    else toDelete.add(FieldKey.COMPOSER);
                }
                if (applyGrouping) {
                    if (!grouping.isBlank()) toSet.put(FieldKey.GROUPING, grouping);
                    else toDelete.add(FieldKey.GROUPING);
                }
                if (applyRating) {
                    if (!rating.isBlank() && !"0".equals(rating)) toSet.put(FieldKey.RATING, rating);
                    else toDelete.add(FieldKey.RATING);
                }
                if (applyBpm) {
                    if (!bpm.isBlank()) toSet.put(FieldKey.BPM, bpm);
                    else toDelete.add(FieldKey.BPM);
                }
                if (applyTotals) {
                    if (discTotal > 0) toSet.put(FieldKey.DISC_TOTAL, String.valueOf(discTotal));
                    if (trackTotal > 0) toSet.put(FieldKey.TRACK_TOTAL, String.valueOf(trackTotal));
                }
                if (applyComment) {
                    if (!comment.isBlank()) toSet.put(FieldKey.COMMENT, comment);
                    else toDelete.add(FieldKey.COMMENT);
                }

                updates.add(new ProcessTagsDialog.SongTagUpdate(
                        song,
                        toSet,
                        toDelete,
                        applyCompilation ? compilation : null,
                        artworkAction,
                        newArtworkBytes,
                        newArtworkMimeType
                ));
            }
        }

        dispose();

        var processDialog = new ProcessTagsDialog(
                getOwner(),
                updates,
                null,
                null
        );
        processDialog.startAndShow();
    }

    private boolean isFieldChecked(String key) {
        JCheckBox cb = applyCheckboxes.get(key);
        return cb != null && cb.isSelected();
    }

    private static String ratingToLabel(String rating) {
        if (rating == null || rating.isBlank() || "0".equals(rating)) {
            return EditAlbumDialog.RATINGS[0];
        }
        try {
            int r = Integer.parseInt(rating);
            if (r >= 90) return EditAlbumDialog.RATINGS[5];
            if (r >= 70) return EditAlbumDialog.RATINGS[4];
            if (r >= 50) return EditAlbumDialog.RATINGS[3];
            if (r >= 30) return EditAlbumDialog.RATINGS[2];
            if (r >= 10) return EditAlbumDialog.RATINGS[1];
        } catch (NumberFormatException ignored) {
        }
        return EditAlbumDialog.RATINGS[0];
    }

    private static String labelToRating(String label) {
        if (label == null || label.startsWith("No rating")) {
            return "";
        }
        if (label.contains("100")) return "100";
        if (label.contains("80")) return "80";
        if (label.contains("60")) return "60";
        if (label.contains("40")) return "40";
        if (label.contains("20")) return "20";
        return "";
    }

    private static Image placeholderArtwork() {
        return new ImageIcon(
                Objects.requireNonNull(
                        AlbumUI.class.getResource("/placeholder_album.png")
                )
        ).getImage();
    }
}
