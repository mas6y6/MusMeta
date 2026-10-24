package com.mas6y6.musmeta.ui;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.ui.dialogs.processing.ProcessMusicProcessingDialog;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class MainWindowTransferHandler extends TransferHandler {
    public static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MainWindowTransferHandler.class);

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDataFlavorSupported(
                DataFlavor.javaFileListFlavor)) {
            return false;
        }

        try {
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>)
                    support.getTransferable().getTransferData(
                            DataFlavor.javaFileListFlavor
                    );

            return !files.isEmpty()
                    && files.stream()
                    .allMatch(file ->
                            file.isFile()
                                    && Constants.isAudioFile(file.toPath())
                    );

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        try {
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>)
                    support.getTransferable().getTransferData(
                            DataFlavor.javaFileListFlavor
                    );

            List<Path> audioFiles = files.stream()
                    .filter(File::isFile)
                    .map(File::toPath)
                    .filter(Constants::isAudioFile)
                    .toList();

            if (audioFiles.isEmpty()) {
                return false;
            }

            LOGGER.info("Importing {} audio files", audioFiles.size());



            var dialog = new ProcessMusicProcessingDialog(MainWindow.INSTANCE, files.toArray(File[]::new));
            dialog.startAndShow();
            MainWindow.INSTANCE.getLibraryUI().refresh();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
