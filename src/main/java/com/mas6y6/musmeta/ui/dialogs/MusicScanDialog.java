package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.core.Core;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialog;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

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
        updateProgress("Scanning for music",null,"");

        Core.scanForMusicFiles(path, Settings.MUSIC_SCAN_IGNORE_PATHS.get());

        return true;
    }
}
