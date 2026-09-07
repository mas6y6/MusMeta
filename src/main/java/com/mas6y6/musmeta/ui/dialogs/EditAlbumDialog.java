package com.mas6y6.musmeta.ui.dialogs;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.mas6y6.musmeta.core.Album;
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

public class EditAlbumDialog extends JDialog {
    private static final Dimension DIALOG_SIZE = new Dimension(680, 520);
    private static final int ARTWORK_SIZE = 180;

    public static final String[] GENRES = {
            "",
            "Alternative", "Ambient", "Blues", "Classical", "Country",
            "Dance", "Electronic", "Folk", "Funk", "Hip-Hop", "House",
            "Indie", "Instrumental", "Jazz", "K-Pop", "Latin", "Metal",
            "Pop", "Punk", "R&B", "Rap", "Reggae", "Rock", "Soul",
            "Soundtrack", "World"
    };

    public static final String[] RATINGS = {
            "No rating",
            "★ (20)",
            "★★ (40)",
            "★★★ (60)",
            "★★★★ (80)",
            "★★★★★ (100)"
    };

    private final Album album;
    private final String initialAlbumTitle;

    // Fields
    private JTextField titleField;
    private JTextField albumArtistField;
    private JTextField artistField;
    private JCheckBox compilationCheck;
    private JComboBox<String> genreCombo;
    private JTextField yearField;
    private JTextField composerField;
    private JTextField groupingField;
    private JComboBox<String> ratingCombo;
    private JTextField bpmField;
    private JSpinner discTotalSpinner;
    private JSpinner trackTotalSpinner;
    private JTextField commentField;

    // Artwork
    private AlbumArtwork artworkLabel;
    private ProcessTagsDialog.ArtworkAction artworkAction = ProcessTagsDialog.ArtworkAction.KEEP;
    private byte[] newArtworkBytes;
    private String newArtworkMimeType;

    public EditAlbumDialog(Window parent, Album album) {
        super(parent, "Album Info", ModalityType.APPLICATION_MODAL);
        this.album = album;
        this.initialAlbumTitle = album.getTitle();

        setSize(DIALOG_SIZE);
        setMinimumSize(new Dimension(620, 480));
        setLocationRelativeTo(parent);

        initFields();
        initComponents();
    }

    private void initFields() {
        titleField = new JTextField(album.getTitle());

        String initialAlbumArtist = "";
        for (Song song : album.getSongs()) {
            String aa = song.getRawAlbumArtist();
            if (!aa.isBlank()) {
                initialAlbumArtist = aa;
                break;
            }
        }
        if (initialAlbumArtist.isBlank() && !album.getArtist().variousArtists()) {
            initialAlbumArtist = album.getArtist().artist();
        }
        albumArtistField = new JTextField(initialAlbumArtist);

        String initialArtist = album.getArtist().variousArtists() ? "" : album.getArtist().artist();
        artistField = new JTextField(initialArtist);

        compilationCheck = new JCheckBox("Compilation of various artists", album.isCompilation());
        compilationCheck.addActionListener(e -> {
            if (compilationCheck.isSelected() && albumArtistField.getText().isBlank()) {
                albumArtistField.setText("Various Artists");
            }
        });

        genreCombo = new JComboBox<>(GENRES);
        genreCombo.setEditable(true);
        String currentGenre = album.getGenre();
        genreCombo.setSelectedItem(currentGenre);

        yearField = new JTextField(album.getYear());
        composerField = new JTextField(album.getComposer());
        groupingField = new JTextField(album.getGrouping());

        ratingCombo = new JComboBox<>(RATINGS);
        ratingCombo.setSelectedItem(ratingToLabel(album.getRating()));

        bpmField = new JTextField(album.getBpm());

        int currentDiscTotal = album.getDiscTotal();
        discTotalSpinner = new JSpinner(new SpinnerNumberModel(Math.max(1, currentDiscTotal), 1, 99, 1));

        int currentTrackTotal = album.getTrackTotal();
        trackTotalSpinner = new JSpinner(new SpinnerNumberModel(Math.max(0, currentTrackTotal), 0, 999, 1));

        commentField = new JTextField(album.getComment());
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(16, 12));
        contentPane.setBorder(new EmptyBorder(16, 20, 16, 20));

        // West: Artwork
        JPanel artworkPanel = createArtworkPanel();
        contentPane.add(artworkPanel, BorderLayout.WEST);

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
        artworkLabel.setArtwork(album.getArtworkImage());
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

        // Album Title
        addFormRow(panel, gbc, row++, "Album:", titleField);

        // Album Artist
        addFormRow(panel, gbc, row++, "Album Artist:", albumArtistField);

        // Artist
        addFormRow(panel, gbc, row++, "Artist:", artistField);

        // Compilation toggle
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(compilationCheck, gbc);
        gbc.gridwidth = 1;

        // Genre
        addFormRow(panel, gbc, row++, "Genre:", genreCombo);

        // Year
        addFormRow(panel, gbc, row++, "Year:", yearField);

        // Composer
        addFormRow(panel, gbc, row++, "Composer:", composerField);

        // Grouping
        addFormRow(panel, gbc, row++, "Grouping:", groupingField);

        // Rating
        addFormRow(panel, gbc, row++, "Rating:", ratingCombo);

        // BPM
        addFormRow(panel, gbc, row++, "BPM:", bpmField);

        // Discs & Tracks totals
        JPanel totalsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        totalsPanel.setOpaque(false);
        totalsPanel.add(new JLabel("Total Discs:"));
        totalsPanel.add(discTotalSpinner);
        totalsPanel.add(Box.createHorizontalStrut(10));
        totalsPanel.add(new JLabel("Total Tracks:"));
        totalsPanel.add(trackTotalSpinner);
        addFormRow(panel, gbc, row++, "Tracks / Discs:", totalsPanel);

        // Comments
        addFormRow(panel, gbc, row++, "Comments:", commentField);

        return panel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, Component comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(labelText), gbc);

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
        String newTitle = titleField.getText().trim();
        if (newTitle.isBlank()) {
            JOptionPane.showMessageDialog(this, "Album title cannot be empty", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String albumArtist = albumArtistField.getText().trim();
        String artist = artistField.getText().trim();
        boolean compilation = compilationCheck.isSelected();
        String genre = genreCombo.getSelectedItem() != null ? genreCombo.getSelectedItem().toString().trim() : "";
        String year = yearField.getText().trim();
        String composer = composerField.getText().trim();
        String grouping = groupingField.getText().trim();
        String rating = labelToRating(ratingCombo.getSelectedItem() != null ? ratingCombo.getSelectedItem().toString() : "");
        String bpm = bpmField.getText().trim();
        int discTotal = (Integer) discTotalSpinner.getValue();
        int trackTotal = (Integer) trackTotalSpinner.getValue();
        String comment = commentField.getText().trim();

        album.setTitle(newTitle);

        List<ProcessTagsDialog.SongTagUpdate> updates = new ArrayList<>();
        List<Song> songs = album.getSongs();

        for (Song song : songs) {
            Map<FieldKey, String> toSet = new HashMap<>();
            Set<FieldKey> toDelete = new HashSet<>();

            toSet.put(FieldKey.ALBUM, newTitle);

            if (!albumArtist.isBlank()) {
                toSet.put(FieldKey.ALBUM_ARTIST, albumArtist);
            } else {
                toDelete.add(FieldKey.ALBUM_ARTIST);
            }

            if (!artist.isBlank()) {
                toSet.put(FieldKey.ARTIST, artist);
            }

            if (!genre.isBlank()) {
                toSet.put(FieldKey.GENRE, genre);
            } else {
                toDelete.add(FieldKey.GENRE);
            }

            if (!year.isBlank()) {
                toSet.put(FieldKey.YEAR, year);
            } else {
                toDelete.add(FieldKey.YEAR);
            }

            if (!composer.isBlank()) {
                toSet.put(FieldKey.COMPOSER, composer);
            } else {
                toDelete.add(FieldKey.COMPOSER);
            }

            if (!grouping.isBlank()) {
                toSet.put(FieldKey.GROUPING, grouping);
            } else {
                toDelete.add(FieldKey.GROUPING);
            }

            if (!rating.isBlank() && !"0".equals(rating)) {
                toSet.put(FieldKey.RATING, rating);
            } else {
                toDelete.add(FieldKey.RATING);
            }

            if (!bpm.isBlank()) {
                toSet.put(FieldKey.BPM, bpm);
            } else {
                toDelete.add(FieldKey.BPM);
            }

            if (discTotal > 0) {
                toSet.put(FieldKey.DISC_TOTAL, String.valueOf(discTotal));
            }

            if (trackTotal > 0) {
                toSet.put(FieldKey.TRACK_TOTAL, String.valueOf(trackTotal));
            }

            if (!comment.isBlank()) {
                toSet.put(FieldKey.COMMENT, comment);
            } else {
                toDelete.add(FieldKey.COMMENT);
            }

            updates.add(new ProcessTagsDialog.SongTagUpdate(
                    song,
                    toSet,
                    toDelete,
                    compilation,
                    artworkAction,
                    newArtworkBytes,
                    newArtworkMimeType
            ));
        }

        dispose();

        var processDialog = new ProcessTagsDialog(
                getOwner(),
                updates,
                album,
                initialAlbumTitle
        );
        processDialog.startAndShow();
    }

    private static String ratingToLabel(String rating) {
        if (rating == null || rating.isBlank() || "0".equals(rating)) {
            return RATINGS[0];
        }
        try {
            int r = Integer.parseInt(rating);
            if (r >= 90) return RATINGS[5];
            if (r >= 70) return RATINGS[4];
            if (r >= 50) return RATINGS[3];
            if (r >= 30) return RATINGS[2];
            if (r >= 10) return RATINGS[1];
        } catch (NumberFormatException ignored) {
        }
        return RATINGS[0];
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
