package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.Main;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialog;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;

import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ConvertImportedSongsDialog extends ProcessingDialog {

    private final List<Song> songs;

    public ConvertImportedSongsDialog(Window owner, List<Song> songs) {
        super(owner, "Converting Music", ProgressMode.DETERMINATE);
        this.songs = List.copyOf(songs);
    }

    /**
     * @return {@code true} when at least one of the songs is in a format other
     *         than the target one, so a selection that needs no work at all
     *         does not flash an empty progress window
     */
    public static boolean isNeeded(List<Song> songs, AlbumFormatNormalizer.AudioFormat target) {
        if (target == null || songs == null || songs.isEmpty()) {
            return false;
        }
        for (Song song : songs) {
            File file = song.getSourceAudioFile();
            if (file != null && !extensionOf(file).equals(target.extension())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean process() {
        Path musicDirectory = Settings.MUSIC_DIRECTORY_PATH.get();
        if (musicDirectory == null || songs.isEmpty()) {
            return true;
        }

        AlbumFormatNormalizer.AudioFormat target =
                AlbumFormatNormalizer.fromSetting(Settings.AUDIO_TARGET_FORMAT.get());

        updateProgress("Converting " + songs.size() + " song(s) to " + target, 0, "");

        int converted = AlbumFormatNormalizer.convertIncompatibleToFolder(
                songs,
                target,
                musicDirectory.toAbsolutePath().normalize(),
                Main.musMetaDirectory,
                (completed, total, details) -> {
                    int percent = total > 0
                            ? (int) Math.round(completed * 100.0 / total)
                            : 100;
                    updateProgress(
                            "Converting " + completed + " of " + total,
                            percent,
                            details != null ? "Converting " + details : details
                    );
                }
        );

        updateProgress("Completed", 100, converted + " of " + songs.size() + " song(s) converted");
        return true;
    }

    @Override
    protected String getProcessTitle() {
        return "Converting Music";
    }

    @Override
    protected String getInitialStatus() {
        return "Preparing conversion...";
    }

    @Override
    protected String getWorkerThreadName() {
        return "Import-Convert-Thread";
    }

    @Override
    protected String getSuccessStatus() {
        return "Songs converted!";
    }

    @Override
    protected String getSuccessDetails() {
        return "The imported songs are ready";
    }

    private static String extensionOf(File file) {
        return com.google.common.io.Files.getFileExtension(file.getName()).toLowerCase(Locale.ROOT);
    }
}
