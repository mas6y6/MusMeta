package com.mas6y6.musmeta.ui.dialogs.processing;

import com.mas6y6.musmeta.Main;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialogBase;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ConvertImportedSongsProcessingDialog extends ProcessingDialogBase {

    private final @NonNull List<Song> songs;

    public ConvertImportedSongsProcessingDialog(Window owner, @NonNull List<Song> songs) {
        super(owner, "Converting Music", ProgressMode.DETERMINATE);
        this.songs = List.copyOf(songs);
    }

    public static boolean isNeeded(@Nullable List<Song> songs, AlbumFormatNormalizer.@Nullable AudioFormat target) {
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
    protected @NonNull String getProcessTitle() {
        return "Converting Music";
    }

    @Override
    protected @NonNull String getInitialStatus() {
        return "Preparing conversion...";
    }

    @Override
    protected @NonNull String getWorkerThreadName() {
        return "Import-Convert-Thread";
    }

    @Override
    protected @NonNull String getSuccessStatus() {
        return "Songs converted!";
    }

    @Override
    protected @NonNull String getSuccessDetails() {
        return "The imported songs are ready";
    }

    private static @NonNull String extensionOf(@NonNull File file) {
        return com.google.common.io.Files.getFileExtension(file.getName()).toLowerCase(Locale.ROOT);
    }
}
