package com.mas6y6.musmeta.ui.dialogs.export;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.MainWindow;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialogBase;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;
import com.mas6y6.musmeta.utils.FFmpegUtils;
import com.mas6y6.musmeta.utils.Utils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportMusicProcessingDialog extends ProcessingDialogBase {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExportMusicProcessingDialog.class);

    private static final String ZIP_EXTENSION = "zip";

    private final List<Song> songs;
    File zipOutput;
    private ZipOutputStream zos;
    private Path conversionDirectory;

    private AlbumFormatNormalizer.@Nullable AudioFormat targetFormat;

    private int failed;

    public ExportMusicProcessingDialog(
            Window owner,
            List<Song> songs
    ) {
        super(owner, "Exporting Music...", ProgressMode.DETERMINATE);
        this.songs = List.copyOf(songs);
    }

    /**
     * @param targetFormat the format the exported songs should be converted to,
     *                     or null to keep their original format
     */
    public void setTargetFormat(AlbumFormatNormalizer.@Nullable AudioFormat targetFormat) {
        this.targetFormat = targetFormat;
    }

    public int getSongCount() {
        return songs.size();
    }

    public String getOutputPath() {
        return zipOutput == null ? "" : zipOutput.getAbsolutePath();
    }

    /**
     * @return true if exporting in {@code format} needs FFmpeg because at least
     * one song is not already stored in it
     */
    public boolean requiresFFmpeg(AlbumFormatNormalizer.@Nullable AudioFormat format) {
        if (format == null || format == targetFormat) {
            return false;
        }

        boolean conversionNeeded = songs.stream()
                .anyMatch(song -> !format.extension().equals(extensionOf(song)));

        return conversionNeeded && FFmpegUtils.getFFmpegExecutable() == null;
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

    private static String entryNameFor(int position, Song song, String extension) {
        String name = Utils.toSafeFilename(
                position + " - " + titleOf(song)
        );

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
        conversionDirectory = Files.createTempDirectory("musmeta-export");

        int exported = 0;

        try {
            zos = new ZipOutputStream(new FileOutputStream(zipOutput));

            for (int i = 0; i < songs.size(); i++) {
                var song = songs.get(i);

                updateProgress(
                        statusOf(),
                        (int) (((double) (i + 1) / songs.size()) * 100),
                        song.getTitle()
                );

                try {
                    zos.putNextEntry(new ZipEntry(
                            entryNameFor(i + 1, song, exportExtensionOf(song))
                    ));

                    Files.copy(contentOf(song), zos);

                    zos.closeEntry();

                    exported++;
                } catch (Exception e) {
                    LOGGER.error("Error exporting song: {}", song.getTitle(), e);
                    failed++;
                }
            }

            zos.finish();
            zos.close();
            zos = null;

            if (exported == 0) {
                throw new IOException("No songs could be exported to " + zipOutput + ".");
            }

            return failed == 0;
        } finally {
            closeZipStream();
            deleteConversionDirectory();
        }
    }

    private String statusOf() {
        return targetFormat == null
                ? "Exporting Music..."
                : "Converting & Exporting as " + targetFormat + "...";
    }

    /**
     * @return the file that should be added to the archive, converted into
     * {@link #conversionDirectory} when the target format requires it
     */
    private Path contentOf(Song song) throws Exception {
        Path source = song.getSourceAudioFile().toPath();

        if (targetFormat == null
                || targetFormat.extension().equals(extensionOf(song))) {
            return source;
        }

        return AlbumFormatNormalizer.convertToTemporaryFile(
                source,
                targetFormat,
                conversionDirectory
        );
    }

    private String exportExtensionOf(Song song) {
        return targetFormat == null ? extensionOf(song) : targetFormat.extension();
    }

    @Override
    protected void onCancelled() {
        closeZipStream();

        deleteConversionDirectory();

        try {
            Files.deleteIfExists(zipOutput.toPath());
        } catch (Exception e) {
            LOGGER.error("Error deleting zip file", e);
        }
    }

    private synchronized void closeZipStream() {
        if (zos == null) {
            return;
        }

        try {
            zos.close();
        } catch (Exception e) {
            LOGGER.error("Error closing ZipOutputStream", e);
        } finally {
            zos = null;
        }
    }

    private synchronized void deleteConversionDirectory() {
        if (conversionDirectory == null) {
            return;
        }

        Path directory = conversionDirectory;
        conversionDirectory = null;

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception e) {
                    LOGGER.warn("Could not delete temporary export file {}", path, e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error deleting temporary export folder", e);
        }
    }

    @Override
    protected String getInitialStatus() {
        return "Preparing export...";
    }

    @Override
    protected String getWorkerThreadName() {
        return "Export-Thread";
    }

    @Override
    protected String getSuccessStatus() {
        return "Songs exported!";
    }

    @Override
    protected String getSuccessDetails() {
        return getOutputPath();
    }

    @Override
    protected String getFailureStatus() {
        return "Some songs could not be exported.";
    }

    @Override
    protected String getFailureDetails() {
        if (failed == 0) {
            return super.getFailureDetails();
        }

        return targetFormat == null
                ? failed + " song(s) could not be exported."
                : failed + " song(s) could not be converted to " + targetFormat + ".";
    }

    @Override
    protected String getFailureDialogTitle() {
        return "Export Incomplete";
    }

    public void openAndStart() {
        SystemFileChooser fc = new SystemFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Export Music...");
        fc.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("Zip Files", "zip"));

        fc.setSelectedFile(new File("export.zip"));
        if( fc.showSaveDialog( MainWindow.INSTANCE ) == SystemFileChooser.APPROVE_OPTION ) {
            this.zipOutput = withZipExtension(fc.getSelectedFile());

            ExportMusicProcessingDialogConfigurationWindow window = new ExportMusicProcessingDialogConfigurationWindow(MainWindow.INSTANCE, this);
            window.setVisible(true);

            if (!window.isConfirmed()) {
                return;
            }

            startAndShow();
        }
    }
}