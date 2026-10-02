package com.mas6y6.musmeta.ui.components;

import com.formdev.flatlaf.util.SystemFileChooser;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class PathField extends JPanel {
    public enum DialogType {
        OPEN,
        SAVE
    }

    public interface PathFieldListener {
        void onPathChanged(String newPath);
    }

    private final JTextField pathField;
    private final JButton browseButton;
    private DialogType dialogType;

    private SystemFileChooser fileChooser;
    private Consumer<SystemFileChooser> fileChooserCustomizer;
    private final ArrayList<PathFieldListener> listeners = new ArrayList<>();

    public PathField(Component parentWindowComponent) {
        this(parentWindowComponent, "Select Folder");
    }

    public PathField(Component parentWindowComponent, String dialogTitle) {
        super(new BorderLayout(10, 0));

        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        pathField = new JTextField();
        browseButton = new JButton("Browse...");

        fileChooser = new SystemFileChooser();
        fileChooser.setDialogTitle(dialogTitle);

        add(pathField, BorderLayout.CENTER);
        add(browseButton, BorderLayout.EAST);

        // Fire our "path" property when the user types something
        pathField.getDocument().addDocumentListener(new DocumentListener() {

            private void changed() {
                firePropertyChange("path", null, getPath());
                firePathChanged(getPath());
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

        browseButton.addActionListener(e -> showFileChooser(parentWindowComponent));
    }

    private void showFileChooser(Component parentComponent) {
        // Start in the currently selected directory if possible
        if (!pathField.getText().isBlank()) {
            File currentPath = new File(pathField.getText());

            if (currentPath.isDirectory()) {
                fileChooser.setCurrentDirectory(currentPath);
            }
        }

        // Let the user customize the chooser immediately before opening it
        if (fileChooserCustomizer != null) {
            fileChooserCustomizer.accept(fileChooser);
        }

        IntSupplier result = new IntSupplier() {
            @Override
            public int getAsInt() {
                return dialogType == DialogType.OPEN ? fileChooser.showOpenDialog(parentComponent) : fileChooser.showSaveDialog(parentComponent);
            }
        };

        if (result.getAsInt() == SystemFileChooser.APPROVE_OPTION) {
            setPath(fileChooser.getSelectedFile().getAbsolutePath());
            firePathChanged(getPath());
        }
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
        firePathChanged(path);
    }

    public void setDialogType(DialogType dialogType) {
        this.dialogType = dialogType;
    }

    public JTextField getTextField() {
        return pathField;
    }

    public JButton getBrowseButton() {
        return browseButton;
    }

    public SystemFileChooser getFileChooser() {
        return fileChooser;
    }

    public void setFileChooser(SystemFileChooser fileChooser) {
        this.fileChooser = Objects.requireNonNull(fileChooser);
    }

    public Consumer<SystemFileChooser> getFileChooserCustomizer() {
        return fileChooserCustomizer;
    }

    public void setFileChooserCustomizer(
            Consumer<SystemFileChooser> fileChooserCustomizer
    ) {
        this.fileChooserCustomizer = fileChooserCustomizer;
    }

    public void addListener(PathFieldListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PathFieldListener listener) {
        listeners.remove(listener);
    }

    private void firePathChanged(String newPath) {
        listeners.forEach(listener -> listener.onPathChanged(newPath));
    }
}