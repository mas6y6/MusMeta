package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.AlbumTags;
import com.mas6y6.musmeta.core.Song;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportTagConflictDialog extends JDialog {

    public enum Choice {
        /**
         * Keep the value the song already carries.
         */
        KEEP_SONG_VALUE,
        /**
         * Overwrite the song's value with the album's.
         */
        USE_ALBUM_VALUE,
        /**
         * Leave the import alone.
         */
        CANCEL
    }

    private static final Dimension DIALOG_SIZE = new Dimension(560, 380);

    private final Album album;
    private final List<Song> songs;
    private final Set<AlbumTags.Essential> conflicts;

    private final Map<AlbumTags.Essential, Choice> decisions = new EnumMap<>(AlbumTags.Essential.class);
    private final List<Runnable> readChoices = new ArrayList<>();

    private boolean cancelled = true;

    /**
     * @return the answer given for each tag, or {@code null} when the import
     *         was cancelled
     */
    public static Map<AlbumTags.Essential, Choice> showAndResolve(
            Window owner,
            List<Song> songs,
            Album album,
            Set<AlbumTags.Essential> conflicts
    ) {
        if (conflicts == null || conflicts.isEmpty() || GraphicsEnvironment.isHeadless()) {
            return null;
        }

        ImportTagConflictDialog dialog = new ImportTagConflictDialog(owner, songs, album, conflicts);
        dialog.setVisible(true);
        return dialog.cancelled ? null : dialog.decisions;
    }

    private ImportTagConflictDialog(
            Window owner,
            List<Song> songs,
            Album album,
            Set<AlbumTags.Essential> conflicts
    ) {
        super(owner, "Song Tags Differ", ModalityType.APPLICATION_MODAL);
        this.songs = songs;
        this.album = album;
        this.conflicts = conflicts;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(DIALOG_SIZE);
        setMinimumSize(DIALOG_SIZE);
        setResizable(false);
        setLocationRelativeTo(owner);

        JPanel page = new JPanel(new BorderLayout(10, 12));
        page.setBorder(new EmptyBorder(20, 25, 20, 25));
        page.add(header(), BorderLayout.NORTH);
        page.add(body(), BorderLayout.CENTER);
        page.add(buttons(), BorderLayout.SOUTH);
        setContentPane(page);
    }

    private JComponent header() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Tags Differ From \"" + escape(album.getTitle()) + "\"");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);

        header.add(Box.createVerticalStrut(8));

        JLabel description = new JLabel(
                "<html><body>"
                        + (songs.size() == 1 ? "This song" : "These " + songs.size() + " songs")
                        + " already state album tags in their own words.<br>"
                        + "For each one, keep what the song has or use the album's value."
                        + "</body></html>"
        );
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(description);

        return header;
    }

    private JComponent body() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        for (AlbumTags.Essential tag : AlbumTags.all()) {
            if (conflicts.contains(tag)) {
                list.add(tagPanel(tag));
                list.add(Box.createVerticalStrut(10));
            }
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JComponent tagPanel(AlbumTags.Essential tag) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel name = new JLabel(tag.label());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(name);

        panel.add(valueLine("This album", List.of(display(tag.albumValue(album))), null));
        panel.add(valueLine(
                songs.size() == 1 ? "This song" : "The songs",
                valuesOrNone(AlbumTags.songValues(songs, tag)),
                "Label.disabledForeground"
        ));

        panel.add(Box.createVerticalStrut(6));

        ButtonGroup group = new ButtonGroup();

        JRadioButton keep = new JRadioButton("Keep the song's own value");
        keep.setSelected(true);
        keep.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(keep);
        panel.add(keep);

        JRadioButton useAlbum = new JRadioButton("Use the album's value: " + display(tag.albumValue(album)));
        useAlbum.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(useAlbum);
        panel.add(useAlbum);

        readChoices.add(() -> decisions.put(
                tag,
                keep.isSelected() ? Choice.KEEP_SONG_VALUE : Choice.USE_ALBUM_VALUE
        ));

        return panel;
    }

    /**
     * One summary line naming a side of the comparison and the values it holds,
     * flagged when the songs disagree among themselves.
     */
    private JComponent valueLine(String side, List<String> values, @Nullable String colorKey) {
        String text = side + ": " + String.join("; ", values);
        if (values.size() > 1) {
            text += "  (" + values.size() + " different)";
        }

        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        if (colorKey != null) {
            label.setForeground(UIManager.getColor(colorKey));
        }
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static List<String> valuesOrNone(List<String> values) {
        return values.isEmpty() ? List.of(display("")) : values;
    }

    private JComponent buttons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JButton apply = new JButton("Import");
        apply.addActionListener(e -> {
            readChoices.forEach(Runnable::run);
            cancelled = false;
            dispose();
        });
        getRootPane().setDefaultButton(apply);

        panel.add(cancel);
        panel.add(apply);
        return panel;
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "(no value)" : value;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
