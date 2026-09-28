package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialogBase;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;
import com.mas6y6.musmeta.utils.Utils;
import org.slf4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportMusicProcessingDialog extends ProcessingDialogBase {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExportMusicProcessingDialog.class);

    private static final String ZIP_EXTENSION = "zip";

    private final List<Song> songs;
    private final File zipOutput;
    private ZipOutputStream zos;

    public ExportMusicProcessingDialog(
            Window owner,
            List<Song> songs,
            File zipOutput
    ) {
        super(owner, "Exporting Music...", ProgressMode.DETERMINATE);
        this.songs = songs;
        this.zipOutput = withZipExtension(zipOutput);
    }

    private static File withZipExtension(File file) {
        String name = file.getName();

        String extension = com.google.common.io.Files
                .getFileExtension(name)
                .toLowerCase(Locale.ROOT);

        if (ZIP_EXTENSION.equals(extension)
                || Constants.MUSIC_EXTENSIONS.contains(extension)) {
            name = name.substring(0, name.length() - extension.length() - 1);
        }

        return new File(
                file.getParentFile(),
                name + "." + ZIP_EXTENSION
        );
    }

    private static String entryNameFor(int position, Song song) {
        String name = Utils.toSafeFilename(
                position + " - " + titleOf(song)
        );

        String extension = extensionOf(song);

        return extension.isBlank() ? name : name + "." + extension;
    }

    private static String titleOf(Song song) {
        String title = song.getTitle();

        int dot = title.lastIndexOf('.');
        if (dot > 0 && Constants.MUSIC_EXTENSIONS.contains(
                title.substring(dot + 1).toLowerCase(Locale.ROOT))) {
            return title.substring(0, dot);
        }

        return title;
    }

    private static String extensionOf(Song song) {
        File source = song.getSourceAudioFile();

        String extension = source == null
                ? ""
                : com.google.common.io.Files
                        .getFileExtension(source.getName())
                        .toLowerCase(Locale.ROOT);

        if (extension.isBlank()) {
            return AlbumFormatNormalizer
                    .fromSetting(Settings.AUDIO_TARGET_FORMAT.get())
                    .extension();
        }

        return extension;
    }

    @Override
    protected boolean process() throws Exception {
        zos = new ZipOutputStream(new FileOutputStream(zipOutput));

        for (int i = 0; i < songs.size(); i++) {
            var song = songs.get(i);

            try {
                updateProgress(
                        "Exporting Music...",
                        (int) (((double) (i + 1) / songs.size()) * 100),
                        song.getTitle()
                );

                zos.putNextEntry(new ZipEntry(entryNameFor(i + 1, song)));

                zos.write(Files.readAllBytes(song.getSourceAudioFile().toPath()));
                zos.closeEntry();
            } catch (Exception e) {
                LOGGER.error("Error exporting song: {}", song.getTitle(), e);
            }
        }

        zos.finish();
        zos.close();

        return true;
    }

    @Override
    protected void onCancelled() {
        try {
            zos.close();
        } catch (Exception e) {
            LOGGER.error("Error closing ZipOutputStream", e);
        }

        try {
            Files.deleteIfExists(zipOutput.toPath());
        } catch (Exception e) {
            LOGGER.error("Error deleting zip file", e);
        }
    }
}
