package com.mas6y6.musmeta.ui.subwindows.settings;

import com.mas6y6.musmeta.Main;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.dialogs.FFmpegDownloadDialog;
import com.mas6y6.musmeta.ui.dialogs.base.EXTDialog;
import com.mas6y6.musmeta.utils.FFmpegUtils;
import com.formdev.flatlaf.util.SystemFileChooser;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FFmpegSettingsTab extends JPanel {

    private final JRadioButton automaticRadio =
            new JRadioButton("Manage automatically");
    private final JRadioButton manualRadio =
            new JRadioButton("Manage manually");

    private final JLabel statusLabel = new JLabel();
    private final JButton repairButton = new JButton("Repair / Reinstall FFmpeg");

    private final JTextField pathField = new JTextField();

    private final JSpinner threadCountSpinner = new JSpinner(
            new SpinnerNumberModel(
                    Math.max(1, Settings.FFMPEG_CONVERSION_THREADS.get()),
                    1,
                    64,
                    1
            )
    );

    public FFmpegSettingsTab() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("FFmpeg");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);

        content.add(Box.createVerticalStrut(20));

        automaticRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(automaticRadio);

        content.add(Box.createVerticalStrut(5));

        JLabel autoDescription = new JLabel(
                "MusMeta downloads and keeps FFmpeg up to date for you. "
                        + "Use the repair button to reinstall it if something breaks."
        );
        autoDescription.setForeground(UIManager.getColor("Label.disabledForeground"));
        autoDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(autoDescription);

        content.add(Box.createVerticalStrut(12));

        JPanel autoActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        autoActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoActions.add(repairButton);
        content.add(autoActions);

        content.add(Box.createVerticalStrut(20));

        manualRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(manualRadio);

        content.add(Box.createVerticalStrut(5));

        JLabel manualDescription = new JLabel(
                "Point MusMeta at an existing FFmpeg installation."
        );
        manualDescription.setForeground(UIManager.getColor("Label.disabledForeground"));
        manualDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(manualDescription);

        content.add(Box.createVerticalStrut(12));

        JPanel pathPanel = new JPanel(new BorderLayout(10, 0));
        pathPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        pathPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton browseButton = new JButton("Browse...");
        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);
        content.add(pathPanel);

        content.add(Box.createVerticalStrut(20));

        JLabel threadLabel = new JLabel("Maximum conversion threads");
        threadLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(threadLabel);

        content.add(Box.createVerticalStrut(5));

        JLabel threadDescription = new JLabel(
                "How many songs FFmpeg may convert at the same time during a scan."
        );
        threadDescription.setForeground(UIManager.getColor("Label.disabledForeground"));
        threadDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(threadDescription);

        JLabel threadLabelWarning = new JLabel("Values beyond 5 are not recommended as your machine may start to lag.");
        threadLabelWarning.setForeground(Color.RED);
        threadLabelWarning.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(threadLabelWarning);

        content.add(Box.createVerticalStrut(10));

        JPanel threadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        threadPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        threadPanel.add(threadCountSpinner);
        content.add(threadPanel);

        content.add(Box.createVerticalStrut(20));

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(statusLabel);

        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.NORTH);

        ButtonGroup group = new ButtonGroup();
        group.add(automaticRadio);
        group.add(manualRadio);

        configureInitialState();

        automaticRadio.addActionListener(e -> setAutomatic(true));
        manualRadio.addActionListener(e -> setAutomatic(false));

        repairButton.addActionListener(e -> runRepair());
        browseButton.addActionListener(e -> browseForFFmpeg());

        threadCountSpinner.addChangeListener(e -> {
            int value = ((Number) threadCountSpinner.getValue()).intValue();
            Settings.FFMPEG_CONVERSION_THREADS.set(Math.max(1, value));
        });

        pathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                onManualPathChanged();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                onManualPathChanged();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                onManualPathChanged();
            }
        });
    }

    private void configureInitialState() {
        boolean auto = Settings.AUTO_FFMPEG_INSTALL.get();
        automaticRadio.setSelected(auto);
        manualRadio.setSelected(!auto);

        String configured = Settings.FFMPEG_INSTALLATION_PATH.get();
        if (configured != null && !configured.isBlank()) {
            pathField.setText(configured);
        } else {
            Path exec = FFmpegUtils.getFFmpegExecutable();
            if (exec != null) {
                pathField.setText(exec.toAbsolutePath().toString());
            }
        }

        applyMode(auto);
        refreshStatus();
    }

    private void setAutomatic(boolean auto) {
        Settings.AUTO_FFMPEG_INSTALL.set(auto);
        applyMode(auto);
        refreshStatus();
    }

    private void applyMode(boolean auto) {
        boolean manual = !auto;
        repairButton.setEnabled(auto);
        pathField.setEnabled(manual);
    }

    private void onManualPathChanged() {
        if (!manualRadio.isSelected()) {
            return;
        }

        String raw = pathField.getText();
        if (raw.isBlank()) {
            Settings.FFMPEG_INSTALLATION_PATH.set("");
            refreshStatus();
            return;
        }

        Path bin = Paths.get(raw);
        Path exec = FFmpegUtils.findFFmpegExecutable(bin);
        if (exec != null && FFmpegUtils.validateFFmpegExecutable(exec)) {
            Settings.FFMPEG_INSTALLATION_PATH.set(exec.toAbsolutePath().toString());
        }
        refreshStatus();
    }

    private void browseForFFmpeg() {
        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle("Select FFmpeg bin directory");
        chooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) != SystemFileChooser.APPROVE_OPTION) {
            return;
        }

        Path bin = chooser.getSelectedFile().toPath();
        Path exec = FFmpegUtils.findFFmpegExecutable(bin);

        if (exec == null) {
            EXTDialog.showMessageDialog(
                    this,
                    "No FFmpeg executable was found in the selected directory.\n"
                            + "Please select a directory that contains the 'ffmpeg' binary.",
                    "FFmpeg Validation",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (!FFmpegUtils.validateFFmpegExecutable(exec)) {
            EXTDialog.showMessageDialog(
                    this,
                    "The FFmpeg executable in the selected directory is invalid or failed to run.",
                    "FFmpeg Validation",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        pathField.setText(exec.toAbsolutePath().toString());
        Settings.FFMPEG_INSTALLATION_PATH.set(exec.toAbsolutePath().toString());
        refreshStatus();
    }

    private void runRepair() {
        if (!automaticRadio.isSelected()) {
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        Path targetDir = Main.appDir.resolve("bins");
        FFmpegDownloadDialog dialog = new FFmpegDownloadDialog(owner, targetDir);
        boolean success = dialog.startAndShow();

        refreshStatus();
        if (success) {
            EXTDialog.showMessageDialog(
                    this,
                    "FFmpeg was reinstalled successfully.",
                    "FFmpeg",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void refreshStatus() {
        Path exec = FFmpegUtils.getFFmpegExecutable();

        if (exec != null) {
            statusLabel.setForeground(UIManager.getColor("Component.infoForeground") != null
                    ? UIManager.getColor("Component.infoForeground")
                    : new Color(46, 139, 87));
            statusLabel.setText("Status: FFmpeg is ready (" + exec.toAbsolutePath() + ")");
        } else {
            statusLabel.setForeground(UIManager.getColor("Component.error.foreground") != null
                    ? UIManager.getColor("Component.error.foreground")
                    : new Color(178, 34, 34));
            statusLabel.setText("Status: FFmpeg is not installed");
        }
    }
}
