package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialogBase;
import com.mas6y6.musmeta.utils.FFmpegUtils;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.nio.file.Path;

public class FFmpegDownloadProcessingDialog extends ProcessingDialogBase {

    private final Path installationDir;

    public FFmpegDownloadProcessingDialog(
            Window owner,
            Path installationDir
    ) {
        super(
                owner,
                Constants.APP_NAME
                        + " - FFmpeg Installation",
                new Dimension(480, 220),
                ProgressMode.DETERMINATE
        );

        this.installationDir = installationDir;
    }

    @Override
    protected @NonNull String getProcessTitle() {
        return "Installing FFmpeg";
    }

    @Override
    protected @NonNull String getInitialStatus() {
        return "Preparing installation...";
    }

    @Override
    protected @NonNull String getWorkerThreadName() {
        return "FFmpeg-Installer-Thread";
    }

    @Override
    protected boolean process()
            throws Exception {

        FFmpegUtils.InstallProgressListener listener =
                new FFmpegUtils.InstallProgressListener() {

                    @Override
                    public void onProgress(
                            String status,
                            int percentage,
                            String details
                    ) {
                        updateProgress(
                                status,
                                percentage,
                                details
                        );
                    }

                    @Override
                    public void onError(
                            String error,
                            Throwable throwable
                    ) {
                        // You could store/log this here if needed.
                    }
                };

        return FFmpegUtils.installFFmpeg(
                installationDir,
                listener
        );
    }

    @Override
    protected @NonNull String getSuccessStatus() {
        return "FFmpeg installation complete!";
    }

    @Override
    protected @NonNull String getFailureStatus() {
        return "Installation failed.";
    }

    @Override
    protected @NonNull String getFailureDialogTitle() {
        return "FFmpeg Installation Error";
    }

    @Override
    protected @NonNull String getFailureMessage() {
        return "Failed to install FFmpeg:\n"
                + (getErrorMessage() != null
                ? getErrorMessage()
                : "Unknown error occurred.");
    }
}