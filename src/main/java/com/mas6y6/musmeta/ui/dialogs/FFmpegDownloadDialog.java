package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialog;
import com.mas6y6.musmeta.utils.FFmpegUtils;

import java.awt.*;
import java.nio.file.Path;

public class FFmpegDownloadDialog extends ProcessingDialog {

    private final Path installationDir;

    public FFmpegDownloadDialog(
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
    protected String getProcessTitle() {
        return "Installing FFmpeg";
    }

    @Override
    protected String getInitialStatus() {
        return "Preparing installation...";
    }

    @Override
    protected String getWorkerThreadName() {
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
    protected String getSuccessStatus() {
        return "FFmpeg installation complete!";
    }

    @Override
    protected String getSuccessDetails() {
        return "Ready to use";
    }

    @Override
    protected String getFailureStatus() {
        return "Installation failed.";
    }

    @Override
    protected String getFailureDialogTitle() {
        return "FFmpeg Installation Error";
    }

    @Override
    protected String getFailureMessage() {
        return "Failed to install FFmpeg:\n"
                + (getErrorMessage() != null
                ? getErrorMessage()
                : "Unknown error occurred.");
    }
}