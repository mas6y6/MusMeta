package com.mas6y6.musmeta.ui.dialogs;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.ui.album.AlbumUI;
import com.mas6y6.musmeta.ui.components.album.AlbumArtwork;
import com.mas6y6.musmeta.ui.dialogs.processing.ProcessTagsProcessingDialog;
import org.jaudiotagger.tag.FieldKey;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class EditSongDialog extends JDialog {
    private static final Dimension DIALOG_SIZE = new Dimension(880, 540);
    private static final int ARTWORK_SIZE = 180;
    private static final String MULTIPLE_VALUES = "Multiple Values";

    private final @NonNull List<Song> songs;
    private final boolean isSingleSong;

    // Single Song Fields
    private JTextField titleField;
    private JSpinner trackNoSpinner;
    private JSpinner trackTotalSpinner;
    private JSpinner discNoSpinner;
    private JSpinner discTotalSpinner;

    // Common / Shared Fields
    private JTextField artistField;
    private JComboBox<String> albumCombo;
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
    private final Set<String> differingFields = new HashSet<>();

    private String lastCommittedAlbum = "";
    private @Nullable Boolean copyArtworkToNewAlbum;
    private boolean suppressAlbumApply;

    private JCheckBox uniformArtistCheck;
    private @Nullable String uniformArtistAlbum;

    // Artwork
    private AlbumArtwork artworkLabel;
    private ProcessTagsProcessingDialog.@NonNull ArtworkAction artworkAction = ProcessTagsProcessingDialog.ArtworkAction.KEEP;
    private byte @Nullable [] newArtworkBytes;
    private @Nullable String newArtworkMimeType;

    public EditSongDialog(Window parent, @NonNull Song song) {
        this(parent, List.of(song));
    }

    public EditSongDialog(Window parent, @NonNull List<Song> songs) {
        super(parent, songs.size() == 1 ? "Song Info" : "Multiple Items Info (" + songs.size() + " songs)", ModalityType.APPLICATION_MODAL);
        this.songs = !songs.isEmpty() ? songs : List.of();
        this.isSingleSong = this.songs.size() == 1;

        setSize(DIALOG_SIZE);
        setMinimumSize(new Dimension(620, 480));
        setLocationRelativeTo(parent);

        initFields();
        initComponents();
    }

    private void initFields() {
        initUniformArtistCheck();

        if (isSingleSong) {
            Song song = songs.getFirst();
            titleField = new JTextField(song.getRawTitle());
            artistField = new JTextField(song.getRawArtist());
            initAlbumCombo(song.getRawAlbum());
            albumArtistField = new JTextField(resolvedAlbumArtist(song));

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
            initMultiFields();
        }
    }

    private void initMultiFields() {
        artistField = newMultipleTextField(songs, Song::getRawArtist, "artist");
        initAlbumCombo(null);
        albumArtistField = newMultipleTextField(songs, EditSongDialog::resolvedAlbumArtist, "albumArtist");

        genreCombo = new JComboBox<>(EditAlbumDialog.GENRES);
        genreCombo.setEditable(true);
        initMultipleCombo(genreCombo, songs, Song::getGenre, "genre");

        yearField = newMultipleTextField(songs, Song::getYear, "year");
        composerField = newMultipleTextField(songs, Song::getComposer, "composer");
        groupingField = newMultipleTextField(songs, Song::getGrouping, "grouping");

        ratingCombo = new JComboBox<>(EditAlbumDialog.RATINGS);
        initMultipleCombo(ratingCombo, songs, s -> ratingToLabel(s.getRating()), "rating");

        bpmField = newMultipleTextField(songs, Song::getBpm, "bpm");

        discTotalSpinner = new JSpinner(new SpinnerNumberModel(Math.max(1, commonInt(songs, Song::getDiscTotal)), 1, 99, 1));
        trackTotalSpinner = new JSpinner(new SpinnerNumberModel(Math.max(0, commonInt(songs, Song::getTrackTotal)), 0, 999, 1));

        boolean anyCompilation = songs.stream().anyMatch(Song::isCompilation);
        boolean allCompilation = songs.stream().allMatch(Song::isCompilation);
        compilationCheck = new JCheckBox("Part of a compilation", allCompilation);
        if (anyCompilation != allCompilation) {
            differingFields.add("compilation");
            compilationCheck.setSelected(false);
        }

        commentField = newMultipleTextField(songs, Song::getComment, "comment");
    }

    /**
     * Builds the album entry as an editable dropdown listing every album in the
     * library, so an existing album can be picked without typing its name.
     * Typing a title the library does not know about opens
     * {@link NewAlbumDialog} to create it, and moving the song off its current
     * album asks whether the artwork should follow.
     *
     * @param currentAlbum the song's album for a single selection, or
     *                     {@code null} to fall back to the multi-song common value
     */
    private void initAlbumCombo(String currentAlbum) {
        albumCombo = new JComboBox<>();
        for (Album album : Library.getInstance().getAlbums()) {
            albumCombo.addItem(album.getTitle());
        }
        albumCombo.setEditable(true);
        albumCombo.setToolTipText("Pick an existing album, or type a new name to create it");

        suppressAlbumApply = true;
        try {
            if (isSingleSong) {
                setComboText(albumCombo, currentAlbum);
            } else {
                initMultipleCombo(albumCombo, songs, Song::getRawAlbum, "album");
            }
        } finally {
            suppressAlbumApply = false;
        }

        lastCommittedAlbum = comboText(albumCombo);

        albumCombo.addActionListener(_ -> commitAlbumSelection());
        if (albumCombo.getEditor().getEditorComponent() instanceof JTextComponent editor) {
            editor.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent ignored) {
                    commitAlbumSelection();
                }
            });
        }
    }

    /**
     * Creates the toggle that collapses a whole album onto a single song artist,
     * the counterpart of iTunes' "Various Artists" switch. Left alone, every
     * song keeps its own artist; ticked, the artist entered above is written to
     * every song of the album and doubles as their album artist.
     */
    private void initUniformArtistCheck() {
        uniformArtistCheck = new JCheckBox("One artist for every song in this album");
        uniformArtistCheck.setToolTipText(
                "Writes the artist above to every song of the album, and uses it as the album artist too"
        );
        uniformArtistCheck.setOpaque(false);
        uniformArtistCheck.addActionListener(_ -> onUniformArtistToggled());
    }

    private void onUniformArtistToggled() {
        if (!uniformArtistCheck.isSelected()) {
            uniformArtistAlbum = null;
            return;
        }

        String album = selectedAlbumTitle();
        if (album.isBlank()) {
            uniformArtistCheck.setSelected(false);
            uniformArtistAlbum = null;
            JOptionPane.showMessageDialog(
                    this,
                    "Pick or name the album first, so the artist can be applied to it.",
                    "No album selected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        uniformArtistAlbum = album;

        // Offer the album's own artist as the shared one, the way iTunes does
        // when a compilation is turned into a single artist album.
        Album target = Library.getInstance().getAlbum(album);
        String albumArtist = target != null ? target.getAlbumArtist() : "";
        if (target != null) {
            uniformArtistCheck.setToolTipText(
                    "Writes the artist above to all " + target.getSongs().size()
                            + " songs of \"" + album + "\""
            );
        }
        if (!albumArtist.isBlank()) {
            setText(artistField, albumArtist);
            setText(albumArtistField, albumArtist);
            tickApply("artist");
        }
    }

    /**
     * @return the album the artist should be applied to, falling back to the
     *         album the selected songs already belong to
     */
    private @NonNull String selectedAlbumTitle() {
        String title = comboText(albumCombo);
        if (title.isBlank() || MULTIPLE_VALUES.equals(title)) {
            return commonAlbumOfSongs(songs);
        }
        return title;
    }

    /**
     * Fired when the user picks from the dropdown or finishes typing in it.
     */
    private void commitAlbumSelection() {        String current = comboText(albumCombo);
        if (current.equals(lastCommittedAlbum)) {
            return;
        }

        String previous = lastCommittedAlbum;
        lastCommittedAlbum = current;
        onAlbumCommitted(previous, current);
    }

    private void onAlbumCommitted(String previous, @NonNull String current) {
        if (current.isBlank() || MULTIPLE_VALUES.equals(current)) {
            return;
        }

        if (!Library.getInstance().containsAlbum(current)) {
            current = promptCreateAlbum(previous, current);
            if (current == null) {
                return;
            }
        }

        if (albumDiffersFromSongs(current)) {
            if (promptArtworkCopy(current) == CopyArtworkDialog.Choice.CANCEL) {
                return;
            }
            inheritAlbumDetails(current);
        }

        if (uniformArtistCheck != null && uniformArtistCheck.isSelected()) {
            onUniformArtistToggled();
        }
    }

    /**
     * Opens {@link NewAlbumDialog} prefilled with the typed title. Returns the
     * title to continue with, or {@code null} when the user backed out.
     */
    private @Nullable String promptCreateAlbum(String previous, String title) {
        NewAlbumDialog dialog = new NewAlbumDialog(ownerWindow());
        dialog.albumNameField.setText(title);
        dialog.setVisible(true);

        Album created = dialog.getCreatedAlbum();
        if (created == null) {
            revertAlbumSelection(previous);
            return null;
        }

        suppressAlbumApply = true;
        try {
            if (findComboIndex(albumCombo, created.getTitle()) < 0) {
                albumCombo.addItem(created.getTitle());
            }
            setComboText(albumCombo, created.getTitle());
        } finally {
            suppressAlbumApply = false;
        }
        lastCommittedAlbum = created.getTitle();

        return created.getTitle();
    }

    private CopyArtworkDialog.Choice promptArtworkCopy(String targetAlbum) {
        Image artwork = songs.isEmpty() ? null : songs.getFirst().getArtworkImage();
        CopyArtworkDialog.Choice choice = CopyArtworkDialog.ask(ownerWindow(), targetAlbum, artwork);

        if (choice != CopyArtworkDialog.Choice.CANCEL) {
            copyArtworkToNewAlbum = choice == CopyArtworkDialog.Choice.COPY;
        } else {
            copyArtworkToNewAlbum = null;
            revertAlbumSelection(commonAlbumOfSongs(songs));
        }

        return choice;
    }

    private void inheritAlbumDetails(String targetAlbum) {
        Album album = Library.getInstance().getAlbum(targetAlbum);
        if (album == null) {
            return;
        }

        // An album without an album artist of its own leaves the field empty,
        // which is then filled in from the song artist when the tags are written.
        setText(albumArtistField, album.getAlbumArtist());

        int discTotal = album.getDiscTotal();
        setSpinner(discTotalSpinner, Math.max(1, discTotal));
        setSpinner(trackTotalSpinner, Math.max(0, album.getTrackTotal()));

        if (isSingleSong) {
            int discNo = Math.clamp(songs.getFirst().getDiscNumber(), 1, discTotal);
            setSpinner(discNoSpinner, discNo);
        }

        // A multi-song selection only writes the fields whose apply box is
        // ticked, so the inherited values need to be ticked as well.
        tickApply("albumArtist");
        tickApply("totals");
    }

    private static void setText(@Nullable JTextField field, @Nullable String value) {
        if (field == null) {
            return;
        }
        field.setText(value == null ? "" : value);
        field.setForeground(UIManager.getColor("TextField.foreground"));
    }

    private static void setSpinner(@Nullable JSpinner spinner, int value) {
        if (spinner == null) {
            return;
        }
        if (spinner.getModel() instanceof SpinnerNumberModel model) {
            value = Math.clamp(value, ((Number) model.getMinimum()).intValue(),
                    ((Number) model.getMaximum()).intValue());
        }
        spinner.setValue(value);
    }

    private void tickApply(String key) {
        JCheckBox check = applyCheckboxes.get(key);
        if (check != null) {
            check.setSelected(true);
        }
    }

    private void revertAlbumSelection(String title) {
        suppressAlbumApply = true;
        try {
            setComboText(albumCombo, title);
        } finally {
            suppressAlbumApply = false;
        }
        lastCommittedAlbum = comboText(albumCombo);
    }

    private @NonNull Window ownerWindow() {
        Window owner = getOwner();
        return owner != null ? owner : this;
    }

    /**
     * @return the album title currently in the combo box, trimmed
     */
    private static @NonNull String comboText(@NonNull JComboBox<String> combo) {
        Object selected = combo.getSelectedItem();
        if (selected == null) {
            selected = combo.isEditable() ? combo.getEditor().getItem() : null;
        }
        return selected == null ? "" : selected.toString().trim();
    }

    /**
     * Shows a title in an editable combo box, adding it to the model when it is
     * a real album that is not already listed. A blank title is only shown in
     * the editor, never as a list entry.
     */
    private static void setComboText(@NonNull JComboBox<String> combo, @Nullable String title) {
        String value = title == null ? "" : title;
        if (!value.isBlank() && findComboIndex(combo, value) < 0) {
            combo.addItem(value);
        }
        combo.setSelectedItem(value);
        combo.getEditor().setItem(value);
    }

    private static int findComboIndex(@NonNull JComboBox<String> combo, String title) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).equalsIgnoreCase(title)) {
                return i;
            }
        }
        return -1;
    }

    private static @NonNull String commonAlbumOfSongs(@NonNull List<Song> songs) {
        return isCommon(songs, Song::getRawAlbum) ? getCommonValue(songs, Song::getRawAlbum) : "";
    }

    /**
     * @return {@code true} when the title is not the album every selected song
     *         currently belongs to
     */
    private boolean albumDiffersFromSongs(@NonNull String title) {
        for (Song song : songs) {
            if (!title.equalsIgnoreCase(song.getRawAlbum())) {
                return true;
            }
        }
        return false;
    }

    private @NonNull JTextField newMultipleTextField(@NonNull List<Song> songs, @NonNull Function<Song, String> extractor, String key) {
        JTextField field = new JTextField(getCommonValue(songs, extractor));
        if (!isCommon(songs, extractor)) {
            differingFields.add(key);
            field.setText(MULTIPLE_VALUES);
            field.setForeground(UIManager.getColor("Label.disabledForeground"));
            field.setToolTipText("The selected songs have different values. Type a value to apply it to all of them.");
        }
        return field;
    }

    private void initMultipleCombo(@NonNull JComboBox<String> combo, @NonNull List<Song> songs, @NonNull Function<Song, String> extractor, String key) {
        if (isCommon(songs, extractor)) {
            combo.setSelectedItem(getCommonValue(songs, extractor));
        } else {
            differingFields.add(key);
            combo.insertItemAt(MULTIPLE_VALUES, 0);
            combo.setSelectedIndex(0);
        }
    }

    private static boolean isCommon(@NonNull List<Song> songs, @NonNull Function<Song, String> extractor) {
        if (songs.isEmpty()) {
            return true;
        }
        String first = extractor.apply(songs.getFirst());
        for (Song song : songs) {
            if (!Objects.equals(first, extractor.apply(song))) {
                return false;
            }
        }
        return true;
    }

    private static int commonInt(@NonNull List<Song> songs, @NonNull ToIntFunction<Song> extractor) {
        if (songs.isEmpty()) {
            return 0;
        }
        int first = extractor.applyAsInt(songs.getFirst());
        for (Song song : songs) {
            if (extractor.applyAsInt(song) != first) {
                return 0;
            }
        }
        return first;
    }

    private static @NonNull String getCommonValue(@NonNull List<Song> songs, @NonNull Function<Song, String> extractor) {
        if (songs.isEmpty()) return "";
        String first = extractor.apply(songs.getFirst());
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

    private @NonNull JPanel createArtworkPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        artworkLabel = new AlbumArtwork(10);
        artworkLabel.setPreferredSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));
        artworkLabel.setMaximumSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));
        artworkLabel.setMinimumSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));

        Image currentImage = null;
        if (isSingleSong && !songs.isEmpty()) {
            currentImage = songs.getFirst().getArtworkImage();
        }
        artworkLabel.setArtwork(currentImage != null ? currentImage : placeholderArtwork());
        artworkLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        artworkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        artworkLabel.setToolTipText("Click or drag an image here to change artwork");

        artworkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(@NonNull MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    chooseArtworkFile();
                }
            }
        });

        artworkLabel.setDropTarget(new DropTarget(artworkLabel, new DropTargetAdapter() {
            @Override
            public void drop(@NonNull DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Object data = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (data instanceof List<?> list && !list.isEmpty()) {
                        Object first = list.getFirst();
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
        chooseButton.addActionListener(_ -> chooseArtworkFile());
        panel.add(chooseButton);

        panel.add(Box.createVerticalStrut(6));

        JButton removeButton = new JButton("Remove Artwork");
        removeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeButton.setMaximumSize(new Dimension(ARTWORK_SIZE, 30));
        removeButton.addActionListener(_ -> {
            artworkAction = ProcessTagsProcessingDialog.ArtworkAction.REMOVE;
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

    private void applyArtworkFile(@NonNull File file) {
        try {
            Image img = ImageIO.read(file);
            if (img != null) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String ext = com.google.common.io.Files.getFileExtension(file.getName()).toLowerCase();
                String mime = "image/png".equals(ext) ? "image/png" : "image/jpeg";
                newArtworkBytes = bytes;
                newArtworkMimeType = mime;
                artworkAction = ProcessTagsProcessingDialog.ArtworkAction.REPLACE;
                artworkLabel.setArtwork(img);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not load image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private @NonNull JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        if (isSingleSong) {
            addFormRow(panel, gbc, row++, "Title:", titleField, null);
            addFormRow(panel, gbc, row++, "Artist:", artistField, null);
            addFormRow(panel, gbc, row++, "Album:", albumCombo, null);
            addAlbumToggleRow(panel, gbc, row++);
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
            addFormRow(panel, gbc, row++, "Album:", albumCombo, "album");
            addAlbumToggleRow(panel, gbc, row++);
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
            compilationCheck.addActionListener(_ -> applyCompCheck.setSelected(true));
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

    private void addFormRow(@NonNull JPanel panel, @NonNull GridBagConstraints gbc, int row, String labelText, Component comp, @Nullable String multiKey) {
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

            if (comp instanceof JTextField tf) {
                tf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) { fieldEdited(tf, applyCheck); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e) { fieldEdited(tf, applyCheck); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { fieldEdited(tf, applyCheck); }
                });
            } else if (comp instanceof JComboBox<?> cb) {
                cb.addActionListener(_ -> {
                    if (!suppressAlbumApply) {
                        applyCheck.setSelected(true);
                    }
                });
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

    private void addAlbumToggleRow(@NonNull JPanel panel, @NonNull GridBagConstraints gbc, int row) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 6, 10, 6);
        panel.add(uniformArtistCheck, gbc);
        gbc.insets = new Insets(5, 6, 5, 6);
    }

    private @NonNull JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(_ -> dispose());

        JButton okButton = new JButton("OK");
        okButton.addActionListener(_ -> onSave());
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

        if (uniformArtistCheck.isSelected() && uniformArtistAlbum != null && uniformArtistValue().isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter the artist to use for \"" + uniformArtistAlbum + "\" or untick the option.",
                    "Artist required",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        List<ProcessTagsProcessingDialog.SongTagUpdate> updates = new ArrayList<>();

        if (isSingleSong) {
            Song song = songs.getFirst();
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

            String album = comboText(albumCombo);
            if (!album.isBlank()) {
                toSet.put(FieldKey.ALBUM, album);
            }

            String albumArtist = albumArtistField.getText().trim();
            if (albumArtist.isBlank()) {
                albumArtist = artist;
            }
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

            updates.add(new ProcessTagsProcessingDialog.SongTagUpdate(
                    song,
                    toSet,
                    toDelete,
                    compilation,
                    artworkAction,
                    newArtworkBytes,
                    newArtworkMimeType
            ));
        } else {
            boolean applyArtist = shouldApply("artist", artistField);
            String artist = artistField.getText().trim();

            boolean applyAlbum = shouldApply("album", albumCombo);
            String album = comboText(albumCombo);

            boolean applyAlbumArtist = shouldApply("albumArtist", albumArtistField);
            String albumArtist = albumArtistField.getText().trim();

            boolean applyGenre = shouldApply("genre", genreCombo);
            String genre = genreCombo.getSelectedItem() != null ? genreCombo.getSelectedItem().toString().trim() : "";

            boolean applyYear = shouldApply("year", yearField);
            String year = yearField.getText().trim();

            boolean applyComposer = shouldApply("composer", composerField);
            String composer = composerField.getText().trim();

            boolean applyGrouping = shouldApply("grouping", groupingField);
            String grouping = groupingField.getText().trim();

            boolean applyRating = shouldApply("rating", ratingCombo);
            String rating = labelToRating(ratingCombo.getSelectedItem() != null ? ratingCombo.getSelectedItem().toString() : "");

            boolean applyBpm = shouldApply("bpm", bpmField);
            String bpm = bpmField.getText().trim();

            boolean applyTotals = isFieldChecked("totals");
            int discTotal = (Integer) discTotalSpinner.getValue();
            int trackTotal = (Integer) trackTotalSpinner.getValue();

            boolean applyCompilation = isFieldChecked("compilation");
            boolean compilation = compilationCheck.isSelected();

            boolean applyComment = shouldApply("comment", commentField);
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
                    String resolved = albumArtist.isBlank() ? artist : albumArtist;
                    if (!resolved.isBlank()) toSet.put(FieldKey.ALBUM_ARTIST, resolved);
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

                updates.add(new ProcessTagsProcessingDialog.SongTagUpdate(
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

        Map<String, byte[]> albumArtwork = buildAlbumArtwork();

        addUniformArtistUpdates(updates);

        dispose();

        var processDialog = new ProcessTagsProcessingDialog(
                getOwner(),
                updates,
                null,
                null,
                albumArtwork.isEmpty() ? null : albumArtwork
        );
        processDialog.startAndShow();
    }

    private @NonNull String uniformArtistValue() {
        String artist = artistField.getText().trim();
        return MULTIPLE_VALUES.equals(artist) ? "" : artist;
    }

    private void addUniformArtistUpdates(@NonNull List<ProcessTagsProcessingDialog.SongTagUpdate> updates) {
        if (!uniformArtistCheck.isSelected() || uniformArtistAlbum == null) {
            return;
        }

        String artist = uniformArtistValue();
        if (artist.isBlank()) {
            return;
        }

        Album album = Library.getInstance().getAlbum(uniformArtistAlbum);
        if (album == null) {
            return;
        }

        Set<Song> alreadyEdited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ProcessTagsProcessingDialog.SongTagUpdate update : updates) {
            alreadyEdited.add(update.song());
        }

        for (Song song : album.getSongs()) {
            if (alreadyEdited.contains(song)) {
                continue;
            }

            Map<FieldKey, String> toSet = new HashMap<>();
            toSet.put(FieldKey.ALBUM, uniformArtistAlbum);
            toSet.put(FieldKey.ARTIST, artist);
            toSet.put(FieldKey.ALBUM_ARTIST, artist);

            updates.add(new ProcessTagsProcessingDialog.SongTagUpdate(
                    song,
                    toSet,
                    Set.of(),
                    null,
                    ProcessTagsProcessingDialog.ArtworkAction.KEEP,
                    null,
                    null
            ));
        }
    }

    private @NonNull Map<String, byte[]> buildAlbumArtwork() {
        if (!Boolean.TRUE.equals(copyArtworkToNewAlbum) || songs.isEmpty()) {
            return Map.of();
        }

        String target = comboText(albumCombo);
        if (target.isBlank() || MULTIPLE_VALUES.equals(target) || !albumDiffersFromSongs(target)) {
            return Map.of();
        }

        byte[] artwork = resolveSourceArtwork();
        return artwork == null ? Map.of() : Map.of(target, artwork);
    }

    private byte[] resolveSourceArtwork() {
        Song song = songs.getFirst();

        byte[] embedded = song.getArtworkData();
        if (embedded != null && embedded.length > 0) {
            return embedded;
        }

        Album source = Library.getInstance().getAlbum(song.getRawAlbum());
        Path cached = source != null ? source.getArtworkPath() : null;
        if (cached != null) {
            try {
                return Files.readAllBytes(cached);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private boolean isFieldChecked(String key) {
        JCheckBox cb = applyCheckboxes.get(key);
        return cb != null && cb.isSelected();
    }

    private boolean shouldApply(String key, @NonNull JTextField field) {
        if (!isFieldChecked(key)) {
            return false;
        }
        return !(differingFields.contains(key) && MULTIPLE_VALUES.equals(field.getText().trim()));
    }

    private boolean shouldApply(String key, @NonNull JComboBox<String> combo) {
        if (!isFieldChecked(key)) {
            return false;
        }
        Object selected = combo.getSelectedItem();
        boolean placeholder = selected != null && MULTIPLE_VALUES.equals(selected.toString().trim());
        return !(differingFields.contains(key) && placeholder);
    }

    private void fieldEdited(@NonNull JTextField field, @NonNull JCheckBox applyCheck) {
        applyCheck.setSelected(true);
        field.setForeground(UIManager.getColor("TextField.foreground"));
    }

    private static String resolvedAlbumArtist(@NonNull Song song) {
        String albumArtist = song.getRawAlbumArtist();
        return albumArtist.isBlank() ? song.getRawArtist() : albumArtist;
    }

    private static String ratingToLabel(@Nullable String rating) {
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

    private static @NonNull String labelToRating(@Nullable String label) {
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
