package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.core.Core;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialog;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer.AudioFormat;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;

public class MusicScanDialog extends ProcessingDialog {
    private Path path;

    public MusicScanDialog(Window owner,boolean useDefaultMusicDir, String path) {
        if (useDefaultMusicDir) {
            this.path = Settings.MUSIC_DIRECTORY_PATH.get();
        } else
            this.path = Path.of(path);

        super(owner, "Scanning Music Library", ProgressMode.INDETERMINATE);
    }

    @Override
    protected boolean process() throws Exception {
        updateProgress("Scanning for music", null, "");

        Core.scanForMusicFiles(path, Settings.MUSIC_SCAN_IGNORE_PATHS.get());

        List<Song> songs = Core.getLibrary();
        Path musicDir = path.toAbsolutePath().normalize();
        Path musMetaDir = musicDir.resolve("MusMeta");

        int converted = AlbumFormatNormalizer.convertIncompatibleToFolder(
                songs,
                AlbumFormatNormalizer.fromSetting(Settings.AUDIO_TARGET_FORMAT.get()),
                musicDir,
                musMetaDir,
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

        if (converted > 0) {
            Library.getInstance().save();
        }

        return true;
    }
}
