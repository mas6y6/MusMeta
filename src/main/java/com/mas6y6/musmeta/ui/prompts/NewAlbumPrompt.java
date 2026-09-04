package com.mas6y6.musmeta.ui.prompts;

import com.mas6y6.musmeta.Main;
import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.ui.components.album.AlbumArtwork;
import com.formdev.flatlaf.util.SystemFileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;


public class NewAlbumPrompt extends JDialog {

    private static final Dimension DIALOG_SIZE = new Dimension(500, 400);
    private static final int ARTWORK_SIZE = 220;

    public final JTextField albumNameField = new JTextField();
    private final AlbumArtwork dropZone = createDropZone();
    private File albumImageFile = null;

    public NewAlbumPrompt(Frame parent) {
        super(parent, "Create new album", true);

        setSize(DIALOG_SIZE);
        setMinimumSize(DIALOG_SIZE);
        setResizable(false);
        setLocationRelativeTo(getOwner());

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );
        content.add(fields(), BorderLayout.CENTER);
        content.add(buttons(), BorderLayout.SOUTH);

        add(content);

        getRootPane().registerKeyboardAction(
                e -> createAlbum(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private AlbumArtwork createDropZone() {
        AlbumArtwork box = new AlbumArtwork(
                "Drag image here or click to select",
                12
        );
        box.setPreferredSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));
        box.setMaximumSize(box.getPreferredSize());
        box.setBorder(
                BorderFactory.createDashedBorder(
                        UIManager.getColor("Component.borderColor"),
                        2, 4, 4, true
                )
        );

        box.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                chooseImage();
            }
        });

        box.setTransferHandler(new ImageDropHandler());

        return box;
    }

    private JPanel fields() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(dropZone, BorderLayout.NORTH);

        JPanel namePanel = new JPanel(new BorderLayout(10, 5));
        namePanel.add(new JLabel("Album name:"), BorderLayout.NORTH);
        namePanel.add(albumNameField, BorderLayout.CENTER);
        panel.add(namePanel, BorderLayout.SOUTH);

        return panel;
    }

    private void chooseImage() {
        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle("Select album image");
        chooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(
                new SystemFileChooser.FileNameExtensionFilter(
                        "Images", "jpg", "jpeg", "png", "gif", "bmp", "webp"
                )
        );

        int result = chooser.showOpenDialog(this);

        if (result == SystemFileChooser.APPROVE_OPTION) {
            setImage(chooser.getSelectedFile());
        }
    }

    private void setImage(File file) {
        albumImageFile = file;

        dropZone.setArtwork(
                new ImageIcon(file.getAbsolutePath()).getImage()
        );
        dropZone.setText("");
        dropZone.setBorder(null);
    }

    public File getAlbumImageFile() {
        return albumImageFile;
    }

    private void createAlbum() {
        String albumName = albumNameField.getText();

        if (albumName.isBlank()) {
            return;
        }

        if (Library.getInstance().containsAlbum(albumName)) {
            JOptionPane.showMessageDialog(this, "Album with title '" + albumName + "' already exists", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            Library.getInstance().registerAlbum(new Album(albumName, storeArtwork(albumName)));
            Library.getInstance().save();
            dispose();
        }
    }

    /**
     * Copies the selected album image into {@code ~/.musmeta/album_art} so it
     * survives restart and can be shown even before the album has any songs.
     *
     * @return the path of the stored image, or {@code null} if no image was chosen
     */
    private Path storeArtwork(String albumName) {
        if (albumImageFile == null) {
            return null;
        }

        try {
            Path artDir = Main.appDir.resolve("album_art");
            Files.createDirectories(artDir);

            String fileName = albumImageFile.getName();
            String extension = com.google.common.io.Files
                    .getFileExtension(fileName)
                    .toLowerCase(Locale.ROOT);

            String baseName = thumbnailName(albumName);

            Path destination = artDir.resolve(
                    extension.isBlank()
                            ? baseName
                            : baseName + "." + extension
            );

            int counter = 1;
            while (Files.exists(destination)) {
                destination = artDir.resolve(
                        extension.isBlank()
                                ? baseName + "_" + counter
                                : baseName + "_" + counter + "." + extension
                );
                counter++;
            }

            Files.copy(
                    albumImageFile.toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return destination;
        } catch (IOException e) {
            return null;
        }
    }

    private static String thumbnailName(String albumName) {
        String sanitized = albumName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .trim();
        return sanitized.isBlank() ? "album" : sanitized;
    }

    private JPanel buttons() {
        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0)
        );

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> createAlbum());

        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);

        return buttonPanel;
    }

    private class ImageDropHandler extends TransferHandler {

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                Transferable transferable = support.getTransferable();
                List<?> files = (List<?>) transferable.getTransferData(
                        DataFlavor.javaFileListFlavor
                );

                if (files.isEmpty()) {
                    return false;
                }

                Object file = files.get(0);
                if (file instanceof File) {
                    setImage((File) file);
                    return true;
                }
            } catch (Exception e) {
                return false;
            }

            return false;
        }
    }
}
