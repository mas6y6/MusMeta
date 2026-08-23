package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.utils.FFmpegUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

public class FFmpegDownloadDialog extends JDialog {

    private static final Dimension DIALOG_SIZE = new Dimension(480, 220);

    private final Path installationDir;
    private final JLabel titleLabel = new JLabel("Installing FFmpeg");
    private final JLabel statusLabel = new JLabel("Preparing installation...");
    private final JLabel detailLabel = new JLabel(" ");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JButton actionButton = new JButton("Cancel");

    private volatile boolean isInstalling = false;
    private volatile boolean isCancelled = false;
    private boolean success = false;
    private String errorMessage = null;
    private Thread workerThread;

    public FFmpegDownloadDialog(Window owner, Path installationDir) {
        super(owner, Constants.APP_NAME + " - FFmpeg Installation", ModalityType.APPLICATION_MODAL);
        this.installationDir = installationDir;

        initUI();
    }

    public FFmpegDownloadDialog(Frame owner, Path installationDir) {
        super(owner, Constants.APP_NAME + " - FFmpeg Installation", true);
        this.installationDir = installationDir;

        initUI();
    }

    public FFmpegDownloadDialog(Dialog owner, Path installationDir) {
        super(owner, Constants.APP_NAME + " - FFmpeg Installation", true);
        this.installationDir = installationDir;

        initUI();
    }

    private void initUI() {
        setSize(DIALOG_SIZE);
        setMinimumSize(DIALOG_SIZE);
        setResizable(false);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleCancel();
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailLabel.setFont(detailLabel.getFont().deriveFont(Font.PLAIN, 11f));
        Color mutedColor = UIManager.getColor("Label.disabledForeground");
        if (mutedColor != null) {
            detailLabel.setForeground(mutedColor);
        }
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        actionButton.addActionListener(e -> handleCancel());
        buttonPanel.add(actionButton);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(progressBar);
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(detailLabel);
        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(buttonPanel);

        setContentPane(mainPanel);
    }

    /**
     * Starts the installation in a background worker and displays the modal dialog.
     * Blocks until the dialog is closed / completed.
     *
     * @return true if installation succeeded, false otherwise.
     */
    public boolean startAndShow() {
        isInstalling = true;
        isCancelled = false;
        success = false;
        errorMessage = null;

        workerThread = new Thread(this::runInstallation, "FFmpeg-Installer-Thread");
        workerThread.setDaemon(true);
        workerThread.start();

        setVisible(true);

        return success;
    }

    private void runInstallation() {
        FFmpegUtils.InstallProgressListener progressListener = new FFmpegUtils.InstallProgressListener() {
            @Override
            public void onProgress(String status, int percentage, String details) {
                if (isCancelled) return;
                SwingUtilities.invokeLater(() -> updateProgress(status, percentage, details));
            }

            @Override
            public void onError(String error, Throwable throwable) {
                errorMessage = error;
            }
        };

        boolean result = false;
        try {
            result = FFmpegUtils.installFFmpeg(installationDir, progressListener);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
            result = false;
        }

        final boolean finalResult = result;
        SwingUtilities.invokeLater(() -> finishInstallation(finalResult));
    }

    public void updateProgress(String status, int percentage, String details) {
        if (status != null && !status.isBlank()) {
            statusLabel.setText(status);
        }
        if (details != null && !details.isBlank()) {
            detailLabel.setText(details);
        } else {
            detailLabel.setText(" ");
        }

        if (percentage >= 0 && percentage <= 100) {
            progressBar.setIndeterminate(false);
            progressBar.setValue(percentage);
            progressBar.setStringPainted(true);
            progressBar.setString(percentage + "%");
        } else {
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(false);
        }
    }

    private void finishInstallation(boolean installSuccess) {
        isInstalling = false;

        if (isCancelled) {
            success = false;
            dispose();
            return;
        }

        if (installSuccess) {
            success = true;
            updateProgress("FFmpeg installation complete!", 100, "Ready to use");
            actionButton.setEnabled(false);

            Timer timer = new Timer(500, e -> dispose());
            timer.setRepeats(false);
            timer.start();
        } else {
            success = false;
            statusLabel.setText("Installation failed.");
            detailLabel.setText(errorMessage != null ? errorMessage : "An unexpected error occurred.");
            progressBar.setIndeterminate(false);
            progressBar.setValue(0);
            progressBar.setStringPainted(false);
            actionButton.setText("Close");
            actionButton.setEnabled(true);

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to install FFmpeg:\n" + (errorMessage != null ? errorMessage : "Unknown error occurred."),
                    "Installation Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void handleCancel() {
        if (!isInstalling) {
            dispose();
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "FFmpeg installation is in progress. Are you sure you want to cancel?",
                "Cancel Installation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            isCancelled = true;
            isInstalling = false;
            if (workerThread != null && workerThread.isAlive()) {
                workerThread.interrupt();
            }
            dispose();
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isInstalling() {
        return isInstalling;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JLabel getDetailLabel() {
        return detailLabel;
    }

    public JButton getActionButton() {
        return actionButton;
    }
}
