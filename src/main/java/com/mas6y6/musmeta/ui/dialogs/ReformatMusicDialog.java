package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialog;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;

import java.awt.Window;
import java.util.List;

public class ReformatMusicDialog extends ProcessingDialog {

    private final List<Song> songs;
    private final AlbumFormatNormalizer.AudioFormat targetFormat;
    private int failed;

    public ReformatMusicDialog(
            Window owner,
            List<Song> songs,
            AlbumFormatNormalizer.AudioFormat targetFormat
    ) {
        super(owner, "Reformatting Music", ProgressMode.DETERMINATE);
        this.songs = List.copyOf(songs);
        this.targetFormat = targetFormat;
    }

    @Override
    protected boolean process() throws Exception {
        updateProgress("Reformatting " + songs.size() + " song(s) to " + targetFormat, 0, "");

        AlbumFormatNormalizer.ConversionResult result = AlbumFormatNormalizer.normalize(
                songs,
                targetFormat,
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

        failed = result.failedFiles().size();
        if (result.converted() > 0) {
            Library.getInstance().save();
        }
        return result.succeeded();
    }

    @Override
    protected String getProcessTitle() {
        return "Reformatting Music";
    }

    @Override
    protected String getInitialStatus() {
        return "Preparing conversion...";
    }

    @Override
    protected String getWorkerThreadName() {
        return "Reformat-Thread";
    }

    @Override
    protected String getSuccessStatus() {
        return "Songs converted!";
    }

    @Override
    protected String getSuccessDetails() {
        return "All songs converted successfully";
    }

    @Override
    protected String getFailureStatus() {
        return "Some songs could not be converted.";
    }

    @Override
    protected String getFailureDetails() {
        return failed > 0
                ? failed + " song(s) could not be converted to " + targetFormat + "."
                : super.getFailureDetails();
    }

    @Override
    protected String getFailureDialogTitle() {
        return "Conversion Incomplete";
    }
}