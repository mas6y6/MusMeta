package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.ui.components.album.AlbumArtwork;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CopyArtworkDialog extends JDialog {

    public enum Choice {
        COPY,
        KEEP_SEPARATE,
        CANCEL
    }

    private static final Dimension DIALOG_SIZE = new Dimension(420, 320);
    private static final int PREVIEW_SIZE = 120;

    private Choice choice = Choice.KEEP_SEPARATE;

    public CopyArtworkDialog(Window owner, String targetAlbum, Image sourceArtwork) {
        super(owner, "Copy Album Artwork?", ModalityType.APPLICATION_MODAL);

        setSize(DIALOG_SIZE);
        setMinimumSize(new Dimension(360, 280));
        setResizable(false);
        setLocationRelativeTo(owner != null ? owner : this);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        content.add(messagePanel(targetAlbum), BorderLayout.NORTH);
        if (sourceArtwork != null) {
            content.add(previewPanel(sourceArtwork), BorderLayout.CENTER);
        }
        content.add(buttonPanel(), BorderLayout.SOUTH);

        setContentPane(content);
    }

    public static Choice ask(Window owner, String targetAlbum, Image sourceArtwork) {
        CopyArtworkDialog dialog = new CopyArtworkDialog(owner, targetAlbum, sourceArtwork);
        dialog.setVisible(true);
        return dialog.getChoice();
    }

    public Choice getChoice() {
        return choice;
    }

    private JPanel messagePanel(String targetAlbum) {
        String title = targetAlbum == null || targetAlbum.isBlank() ? "the new album" : targetAlbum;

        JLabel label = new JLabel(
                "<html><div style='width:320px'>"
                        + "The selected song will be moved to <b>" + escape(title) + "</b>.<br><br>"
                        + "Copy the current album artwork over to it?"
                        + "</div></html>"
        );
        label.setHorizontalAlignment(SwingConstants.LEADING);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }

    private JPanel previewPanel(Image sourceArtwork) {
        AlbumArtwork preview = new AlbumArtwork(8);
        preview.setPreferredSize(new Dimension(PREVIEW_SIZE, PREVIEW_SIZE));
        preview.setMaximumSize(new Dimension(PREVIEW_SIZE, PREVIEW_SIZE));
        preview.setMinimumSize(new Dimension(PREVIEW_SIZE, PREVIEW_SIZE));
        preview.setAlignmentX(Component.CENTER_ALIGNMENT);
        preview.setArtwork(sourceArtwork);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel caption = new JLabel("Current artwork");
        caption.setAlignmentX(Component.CENTER_ALIGNMENT);
        caption.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(caption);
        panel.add(Box.createVerticalStrut(6));
        panel.add(preview);

        return panel;
    }

    private JPanel buttonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setOpaque(false);

        JButton copyButton = new JButton("Copy Artwork");
        copyButton.addActionListener(e -> {
            choice = Choice.COPY;
            dispose();
        });

        JButton keepButton = new JButton("Don't Copy");
        keepButton.addActionListener(e -> {
            choice = Choice.KEEP_SEPARATE;
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            choice = Choice.CANCEL;
            dispose();
        });

        panel.add(copyButton);
        panel.add(keepButton);
        panel.add(cancelButton);

        return panel;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
