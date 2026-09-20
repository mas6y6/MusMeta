package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.core.Duplicates;
import com.mas6y6.musmeta.core.Song;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Asks the user to pick, for every song that collides during import or scan,
 * which file should end up in the library. Each collision is a small radio
 * group: "keep existing", one entry per incoming file, and optionally
 * "skip". The dialog is modal and blocks the worker thread until a choice
 * is made; a {@code null} return means the whole import should be aborted.
 */
public class DuplicateImportDialog extends JDialog {

    private static final Dimension DIALOG_SIZE = new Dimension(640, 460);

    private final Map<Duplicates.Group, Song> choices = new HashMap<>();
    private final List<GroupSpec> specs = new ArrayList<>();
    private boolean cancelled = true;

    private record Option(Song song, String label) {
    }

    private record GroupSpec(Duplicates.Group group, List<Option> options, List<JRadioButton> buttons) {
    }

    /**
     * Shows the dialog and returns, for every duplicate group, the song the
     * user chose to import. A group mapped to {@code null} means the user
     * chose "keep existing" or "skip" (import nothing for that group).
     * Returns {@code null} when the user cancelled the whole import, or when
     * there is nothing to resolve.
     */
    public static Map<Duplicates.Group, Song> showAndResolve(
            Window owner,
            List<Duplicates.Group> groups
    ) {
        if (groups == null || groups.isEmpty()) {
            return Map.of();
        }
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }

        final Map<Duplicates.Group, Song>[] result = new Map[1];

        try {
            SwingUtilities.invokeAndWait(() -> {
                DuplicateImportDialog dialog =
                        new DuplicateImportDialog(owner, groups);
                dialog.setVisible(true);
                result[0] = dialog.cancelled ? null : dialog.choices;
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }

        return result[0];
    }

    private DuplicateImportDialog(Window owner, List<Duplicates.Group> duplicateGroups) {
        super(owner, "Resolve Duplicate Songs", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setSize(DIALOG_SIZE);
        setMinimumSize(DIALOG_SIZE);
        setLocationRelativeTo(owner);

        JPanel page = new JPanel(new BorderLayout(10, 12));
        page.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        page.add(header(duplicateGroups), BorderLayout.NORTH);
        page.add(body(duplicateGroups), BorderLayout.CENTER);
        page.add(buttons(), BorderLayout.SOUTH);
        setContentPane(page);
    }

    private JComponent header(List<Duplicates.Group> duplicateGroups) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Duplicate songs found");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        long songs = duplicateGroups.stream()
                .mapToLong(g -> g.incoming().size())
                .sum();

        JLabel description = new JLabel(
                "<html><body>" + songs + " file(s) from your selection would import a song that already "
                        + "exists in your library, or that is selected more than once. Choose which file "
                        + "to import for each song.</body></html>"
        );
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(description);
        return header;
    }

    private JComponent body(List<Duplicates.Group> duplicateGroups) {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        for (Duplicates.Group group : duplicateGroups) {
            list.add(groupPanel(group));
            list.add(Box.createVerticalStrut(10));
        }

        return new JScrollPane(list);
    }

    private JComponent groupPanel(Duplicates.Group group) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        Song reference = group.incoming().get(0);
        JLabel title = new JLabel(trackTitle(reference));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);

        JLabel album = new JLabel(albumName(reference));
        album.setFont(album.getFont().deriveFont(Font.PLAIN, 10f));
        album.setForeground(UIManager.getColor("Label.disabledForeground"));
        album.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(album);

        panel.add(Box.createVerticalStrut(6));

        ButtonGroup buttons = new ButtonGroup();
        List<Option> options = new ArrayList<>();
        List<JRadioButton> radioButtons = new ArrayList<>();

        if (!group.existing().isEmpty()) {
            options.add(new Option(group.existing().get(0), keepLabel(group.existing())));
        }
        for (Song incoming : group.incoming()) {
            options.add(new Option(incoming, "Import: " + fileName(incoming)));
        }
        boolean skipNeeded = group.incoming().size() > 1 || !group.existing().isEmpty();
        if (skipNeeded) {
            options.add(new Option(null, "Skip (don't import this song)"));
        }
        if (!group.existing().isEmpty()) {
            options.add(new Option(null, "Skip (don't import this song)"));
        }

        for (int i = 0; i < options.size(); i++) {
            JRadioButton radio = new JRadioButton(options.get(i).label());
            radio.setSelected(i == 0);
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            radio.setFont(radio.getFont().deriveFont(Font.PLAIN, 11f));
            buttons.add(radio);
            radioButtons.add(radio);
            panel.add(radio);
        }

        specs.add(new GroupSpec(group, options, radioButtons));
        return panel;
    }

    private JComponent buttons() {
        JButton apply = new JButton("Apply");
        apply.addActionListener(e -> {
            for (GroupSpec spec : specs) {
                for (int i = 0; i < spec.buttons().size(); i++) {
                    if (spec.buttons().get(i).isSelected()) {
                        if (i < spec.options().size()) {
                            choices.put(spec.group(), spec.options().get(i).song());
                        }
                        break;
                    }
                }
            }
            cancelled = false;
            dispose();
        });

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.add(cancel);
        panel.add(apply);
        return panel;
    }

    private static String trackTitle(Song song) {
        String title = song.getTitle();
        if (song.getArtist() != null && !song.getArtist().isBlank()) {
            return title + "  —  " + song.getArtist();
        }
        return title;
    }

    private static String albumName(Song song) {
        return "Album: " + song.getAlbum();
    }

    private static String keepLabel(List<Song> existing) {
        if (existing.size() == 1) {
            return "Keep existing: " + fileName(existing.get(0));
        }
        return "Keep existing (" + existing.size() + " songs already in library)";
    }

    private static String fileName(Song song) {
        return song.getAudioFile().getFile().getName();
    }
}
