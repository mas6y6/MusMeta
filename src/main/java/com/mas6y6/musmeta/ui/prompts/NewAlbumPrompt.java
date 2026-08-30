package com.mas6y6.musmeta.ui.prompts;

import com.mas6y6.musmeta.ui.dialogs.MusicScanDialog;

import javax.swing.*;
import java.awt.*;


public class NewAlbumPrompt extends JDialog {

    private static final Dimension DIALOG_SIZE = new Dimension(500, 180);

    public final JTextField albumNameField = new JTextField();

    public NewAlbumPrompt(Frame parent) {
        super(parent, "Create new album", true);

        setSize(DIALOG_SIZE);
        setMinimumSize(DIALOG_SIZE);
        setResizable(false);
        setLocationRelativeTo(getOwner());

        albumNameField.setSize(new Dimension(200, 50));

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );
        content.add(new JLabel("Album name:"), BorderLayout.NORTH);
        content.add(albumNameField, BorderLayout.CENTER);
        content.add(buttons(), BorderLayout.SOUTH);

        add(content);
    }

    private JPanel buttons() {
        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0)
        );

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> {
            String albumName = albumNameField.getText();

            if (albumName.isBlank()) {
                return;
            }

            // Create album here

            dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);

        return buttonPanel;
    }
}
