package com.mas6y6.musmeta.ui.prompts;

import com.mas6y6.musmeta.core.Library.MissingSong;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Informs the user that songs from the saved library could not be found
 * (or read) at the path they were stored.
 */
public class MissingSongsPrompt extends JDialog {
    private static final Dimension DIALOG_SIZE = new Dimension(580, 430);

    public MissingSongsPrompt(JFrame parent, List<MissingSong> missingSongs) {
        super(parent, "Missing Songs", true);

        setSize(DIALOG_SIZE);
        setMinimumSize(DIALOG_SIZE);
        setLocationRelativeTo(parent);

        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Some songs could not be found");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel(
                "<html>The following " + missingSongs.size() + " song(s) from your library could not be "
                        + "found (or read) at their saved path.<br>"
                        + "The files may have been moved or deleted.</html>");
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(description);

        page.add(header, BorderLayout.NORTH);

        JTextArea listArea = new JTextArea();
        listArea.setEditable(false);
        listArea.setLineWrap(true);
        listArea.setWrapStyleWord(true);
        listArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        StringBuilder text = new StringBuilder();
        for (MissingSong song : missingSongs) {
            text.append(song.path());
            if (song.albumTitle() != null && !song.albumTitle().isBlank()) {
                text.append("   —   ").append(song.albumTitle());
                if (song.discIndex() > 0) {
                    text.append(" (Disc ").append(song.discIndex()).append(")");
                }
            }
            text.append(System.lineSeparator());
        }
        listArea.setText(text.toString());
        listArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(listArea);
        page.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.add(closeButton);
        page.add(buttons, BorderLayout.SOUTH);

        setContentPane(page);
    }
}