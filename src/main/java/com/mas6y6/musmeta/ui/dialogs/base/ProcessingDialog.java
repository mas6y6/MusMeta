package com.mas6y6.musmeta.ui.dialogs.base;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public abstract class ProcessingDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingDialog.class.getName());

    public enum ProgressMode {
        INDETERMINATE,
        DETERMINATE,
        HIDDEN
    }

    private static final Dimension DEFAULT_SIZE =
            new Dimension(480, 220);

    private final Window owner;
    private final String title;
    private final Dimension size;
    private final ProgressMode progressMode;

    private JDialog dialog;

    private final JLabel titleLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JLabel detailLabel = new JLabel(" ");
    private final JProgressBar progressBar =
            new JProgressBar(0, 100);

    private final JButton actionButton =
            new JButton("Cancel");

    private volatile boolean processing;
    private volatile boolean cancelled;
    private volatile boolean success;

    private volatile String errorMessage;

    private Thread workerThread;

    protected ProcessingDialog(
            Window owner,
            String title
    ) {
        this(
                owner,
                title,
                DEFAULT_SIZE,
                ProgressMode.INDETERMINATE
        );
    }

    protected ProcessingDialog(
            Window owner,
            String title,
            ProgressMode progressMode
    ) {
        this(
                owner,
                title,
                DEFAULT_SIZE,
                progressMode
        );
    }

    protected ProcessingDialog(
            Window owner,
            String title,
            Dimension size,
            ProgressMode progressMode
    ) {
        this.owner = owner;
        this.title = title;
        this.size = size;
        this.progressMode = progressMode;

        JPanel mainPanel = initUI(progressMode);

        if (!GraphicsEnvironment.isHeadless()) {
            dialog = new JDialog(
                    owner,
                    title,
                    Dialog.ModalityType.APPLICATION_MODAL
            );

            dialog.setSize(size);
            dialog.setMinimumSize(size);
            dialog.setResizable(false);
            dialog.setLocationRelativeTo(owner);
            dialog.setDefaultCloseOperation(
                    JDialog.DO_NOTHING_ON_CLOSE
            );
            dialog.setContentPane(mainPanel);

            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    cancel();
                }
            });
        }
    }

    public JDialog getDialog() {
        return dialog;
    }

    public void setVisible(boolean visible) {
        if (dialog != null) {
            dialog.setVisible(visible);
        }
    }

    public void dispose() {
        if (dialog != null) {
            dialog.dispose();
        }
    }

    private JPanel initUI(ProgressMode progressMode) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(
                new BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        );

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        20, 25, 20, 25
                )
        );

        titleLabel.setText(getProcessTitle());
        titleLabel.setFont(
                titleLabel.getFont().deriveFont(
                        Font.BOLD,
                        18f
                )
        );
        titleLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        statusLabel.setText(
                getInitialStatus()
        );
        statusLabel.setFont(
                statusLabel.getFont().deriveFont(
                        Font.PLAIN,
                        13f
                )
        );
        statusLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        detailLabel.setFont(
                detailLabel.getFont().deriveFont(
                        Font.PLAIN,
                        11f
                )
        );

        Color mutedColor =
                UIManager.getColor(
                        "Label.disabledForeground"
                );

        if (mutedColor != null) {
            detailLabel.setForeground(mutedColor);
        }

        detailLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        progressBar.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        progressBar.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        24
                )
        );

        configureProgressBar(progressMode);

        JPanel buttonPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        0,
                        0
                )
        );

        buttonPanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        buttonPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        35
                )
        );

        actionButton.addActionListener(
                e -> cancel()
        );

        buttonPanel.add(actionButton);

        mainPanel.add(titleLabel);

        mainPanel.add(
                Box.createVerticalStrut(10)
        );

        mainPanel.add(statusLabel);

        mainPanel.add(
                Box.createVerticalStrut(6)
        );

        if (progressMode != ProgressMode.HIDDEN) {
            mainPanel.add(progressBar);

            mainPanel.add(
                    Box.createVerticalStrut(6)
            );
        }

        mainPanel.add(detailLabel);

        mainPanel.add(
                Box.createVerticalGlue()
        );

        mainPanel.add(buttonPanel);

        return mainPanel;
    }

    private void configureProgressBar(
            ProgressMode mode
    ) {
        switch (mode) {

            case INDETERMINATE -> {
                progressBar.setIndeterminate(true);
                progressBar.setStringPainted(false);
            }

            case DETERMINATE -> {
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
                progressBar.setStringPainted(true);
                progressBar.setString("0%");
            }

            case HIDDEN -> {
                progressBar.setVisible(false);
            }
        }
    }

    /**
     * Starts the process and displays the dialog.
     *
     * @return true if the process succeeded.
     */
    public final boolean startAndShow() {
        if (processing) {
            throw new IllegalStateException(
                    "Process is already running"
            );
        }

        cancelled = false;
        success = false;
        errorMessage = null;
        processing = true;

        if (GraphicsEnvironment.isHeadless()) {
            try {
                success = process();
            } catch (InterruptedException e) {
                cancelled = true;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOGGER.error("Error processing files", e);
                errorMessage =
                        e.getMessage() != null
                                ? e.getMessage()
                                : e.toString();
                success = false;
            } finally {
                processing = false;
            }
            return success;
        }

        resetUI();

        workerThread = new Thread(
                this::runProcess,
                getWorkerThreadName()
        );

        workerThread.setDaemon(true);
        workerThread.start();

        if (dialog != null) {
            dialog.setVisible(true);
        }

        return success;
    }

    private void runProcess() {
        boolean result = false;

        try {
            result = process();
        } catch (InterruptedException e) {
            cancelled = true;
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.error("Error processing files", e);
            errorMessage =
                    e.getMessage() != null
                            ? e.getMessage()
                            : e.toString();
        }

        boolean finalResult = result;

        SwingUtilities.invokeLater(() ->
                finishProcess(finalResult)
        );
    }

    /**
     * The actual work performed by the dialog.
     *
     * This method runs on the worker thread.
     */
    protected abstract boolean process()
            throws Exception;

    protected String getProcessTitle() {
        return "Processing";
    }

    protected String getInitialStatus() {
        return "Preparing...";
    }

    protected String getWorkerThreadName() {
        return "Processing-Thread";
    }

    private void finishProcess(boolean result) {
        processing = false;

        if (cancelled) {
            success = false;
            onCancelled();
            dispose();
            return;
        }

        if (result) {
            success = true;
            onSuccess();
        } else {
            success = false;
            onFailure();
        }
    }

    protected void onSuccess() {
        updateProgress(
                getSuccessStatus(),
                100,
                getSuccessDetails()
        );

        actionButton.setEnabled(false);

        if (!GraphicsEnvironment.isHeadless() && dialog != null) {
            Timer timer = new Timer(
                    getSuccessCloseDelay(),
                    e -> dispose()
            );

            timer.setRepeats(false);
            timer.start();
        }
    }

    protected void onFailure() {
        statusLabel.setText(
                getFailureStatus()
        );

        detailLabel.setText(
                errorMessage != null
                        ? errorMessage
                        : getFailureDetails()
        );

        actionButton.setText("Close");
        actionButton.setEnabled(true);

        if (!GraphicsEnvironment.isHeadless() && dialog != null) {
            EXTDialog.showMessageDialog(
                    dialog,
                    getFailureMessage(),
                    getFailureDialogTitle(),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    protected void onCancelled() {
    }

    protected String getSuccessStatus() {
        return "Operation complete!";
    }

    protected String getSuccessDetails() {
        return "Ready to use";
    }

    protected String getFailureStatus() {
        return "Operation failed.";
    }

    protected String getFailureDetails() {
        return "An unexpected error occurred.";
    }

    protected String getFailureMessage() {
        return errorMessage != null
                ? errorMessage
                : "An unexpected error occurred.";
    }

    protected String getFailureDialogTitle() {
        return "Operation Failed";
    }

    protected int getSuccessCloseDelay() {
        return 500;
    }

    protected void resetUI() {
        actionButton.setText("Cancel");
        actionButton.setEnabled(true);

        statusLabel.setText(
                getInitialStatus()
        );

        detailLabel.setText(" ");
    }

    /**
     * Updates the progress UI.
     *
     * This can safely be called from the worker thread.
     */
    protected final void updateProgress(
            String status,
            @Nullable Integer percentage,
            String details
    ) {
        if (GraphicsEnvironment.isHeadless()) {
            if (status != null && !status.isBlank()) {
                statusLabel.setText(status);
            }
            if (details != null && !details.isBlank()) {
                detailLabel.setText(details);
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {

            if (status != null && !status.isBlank()) {
                statusLabel.setText(status);
            }

            if (details != null && !details.isBlank()) {
                detailLabel.setText(details);
            } else {
                detailLabel.setText(" ");
            }

            if (percentage != null) {
                if (percentage >= 0 &&
                        percentage <= 100) {

                    progressBar.setIndeterminate(false);
                    progressBar.setValue(percentage);
                    progressBar.setStringPainted(true);
                    progressBar.setString(
                            percentage + "%"
                    );

                } else {
                    progressBar.setIndeterminate(true);
                    progressBar.setStringPainted(false);
                }
            } else {
                progressBar.setIndeterminate(true);
                progressBar.setStringPainted(false);
            }
        });
    }

    private void cancel() {
        if (!processing) {
            dispose();
            return;
        }

        if (!GraphicsEnvironment.isHeadless() && dialog != null) {
            int choice = EXTDialog.showConfirmDialog(
                    dialog,
                    getCancelMessage(),
                    "Cancel Operation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        cancelled = true;
        processing = false;

        if (workerThread != null &&
                workerThread.isAlive()) {

            workerThread.interrupt();
        }

        onCancelled();
        dispose();
    }

    protected String getCancelMessage() {
        return "The operation is still in progress. "
                + "Are you sure you want to cancel?";
    }

    public final boolean isProcessing() {
        return processing;
    }

    public final boolean isCancelled() {
        return cancelled;
    }

    public final boolean isSuccess() {
        return success;
    }

    public final String getErrorMessage() {
        return errorMessage;
    }

    protected final JLabel getStatusLabel() {
        return statusLabel;
    }

    protected final JLabel getDetailLabel() {
        return detailLabel;
    }

    protected final JProgressBar getProgressBar() {
        return progressBar;
    }

    protected final JButton getActionButton() {
        return actionButton;
    }
}