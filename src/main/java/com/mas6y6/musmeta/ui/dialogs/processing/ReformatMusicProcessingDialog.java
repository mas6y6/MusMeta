package com.mas6y6.musmeta.ui.dialogs.processing;

import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.ui.MainWindow;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialogBase;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;
import org.jspecify.annotations.NonNull;

import java.awt.Window;
import java.util.List;

public class ReformatMusicProcessingDialog extends ProcessingDialogBase {

    private final @NonNull List<Song> songs;
    private final AlbumFormatNormalizer.AudioFormat targetFormat;
    private int failed;

    public ReformatMusicProcessingDialog(
            Window owner,
            @NonNull List<Song> songs,
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

        MainWindow.INSTANCE.refreshLibraryAndTabs();

        return result.succeeded();
    }

    @Override
    protected @NonNull String getProcessTitle() {
        return "Reformatting Music";
    }

    @Override
    protected @NonNull String getInitialStatus() {
        return "Preparing conversion...";
    }

    @Override
    protected @NonNull String getWorkerThreadName() {
        return "Reformat-Thread";
    }

    @Override
    protected @NonNull String getSuccessStatus() {
        return "Songs converted!";
    }

    @Override
    protected @NonNull String getSuccessDetails() {
        return "All songs converted successfully";
    }

    @Override
    protected @NonNull String getFailureStatus() {
        return "Some songs could not be converted.";
    }

    @Override
    protected String getFailureDetails() {
        return failed > 0
                ? failed + " song(s) could not be converted to " + targetFormat + "."
                : super.getFailureDetails();
    }

    @Override
    protected @NonNull String getFailureDialogTitle() {
        return "Conversion Incomplete";
    }
}