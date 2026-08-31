package com.mas6y6.musmeta.ui.components;

import com.formdev.flatlaf.util.SystemFileChooser;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.Objects;

public class PathField extends JPanel {

    private final JTextField pathField;
    private final JButton browseButton;

    public PathField(Component parentComponent) {
        this(parentComponent, "Select Folder");
    }

    public PathField(Component parentComponent, String dialogTitle) {
        super(new BorderLayout(10, 0));

        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        pathField = new JTextField();
        browseButton = new JButton("Browse...");

        add(pathField, BorderLayout.CENTER);
        add(browseButton, BorderLayout.EAST);

        // Fire our "path" property when the user types something
        pathField.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                firePropertyChange("path", null, getPath());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        });

        browseButton.addActionListener(e -> {
            SystemFileChooser chooser = new SystemFileChooser();

            chooser.setDialogTitle(dialogTitle);
            chooser.setFileSelectionMode(
                    SystemFileChooser.DIRECTORIES_ONLY
            );
            chooser.setAcceptAllFileFilterUsed(false);

            // Start in the currently selected directory if possible
            if (!pathField.getText().isBlank()) {
                File currentPath = new File(pathField.getText());

                if (currentPath.isDirectory()) {
                    chooser.setCurrentDirectory(currentPath);
                }
            }

            int result = chooser.showOpenDialog(parentComponent);

            if (result == SystemFileChooser.APPROVE_OPTION) {
                setPath(
                        chooser.getSelectedFile().getAbsolutePath()
                );
            }
        });
    }

    public String getPath() {
        return pathField.getText();
    }

    public void setPath(String path) {
        String oldPath = getPath();
        String newPath = path != null ? path : "";

        if (Objects.equals(oldPath, newPath)) {
            return;
        }

        pathField.setText(newPath);
        firePropertyChange("path", oldPath, newPath);
    }

    public JTextField getTextField() {
        return pathField;
    }

    public JButton getBrowseButton() {
        return browseButton;
    }
}