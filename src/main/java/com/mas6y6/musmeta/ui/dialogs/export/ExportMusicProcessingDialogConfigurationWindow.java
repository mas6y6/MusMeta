package com.mas6y6.musmeta.ui.dialogs.export;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.mas6y6.musmeta.ui.components.PathField;
import com.mas6y6.musmeta.ui.dialogs.base.MDialog;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

public class ExportMusicProcessingDialogConfigurationWindow extends JDialog {
    private final ExportMusicProcessingDialog processingDialog;
    private static final Dimension DIALOG_SIZE = new Dimension(600, 350);

    private static final class FormatOption {
        private final String label;
        private final AlbumFormatNormalizer.AudioFormat format;

        private FormatOption(String label, AlbumFormatNormalizer.AudioFormat format) {
            this.label = label;
            this.format = format;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final JComboBox<FormatOption> formatPicker = new JComboBox<>();
    private final JLabel formatDetails = new JLabel(" ");

    private boolean confirmed;

    public ExportMusicProcessingDialogConfigurationWindow(Window owner, ExportMusicProcessingDialog processingDialog) {
        super(owner, "Export", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setSize(DIALOG_SIZE);
        setMinimumSize(DIALOG_SIZE);
        setLocationRelativeTo(owner != null ? owner : this);

        this.processingDialog = processingDialog;

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        );

        JLabel title = new JLabel("Export Music");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(title);

        mainPanel.add(Box.createVerticalStrut(15));

        JLabel summary = new JLabel(processingDialog.getSongCount() + " song(s) will be exported to:");
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(summary);

        PathField destination = new PathField(this, "Export Music...");
        destination.setPath(String.valueOf(processingDialog.zipOutput));
        destination.setAlignmentX(Component.LEFT_ALIGNMENT);
        destination.setDialogType(PathField.DialogType.SAVE);
        destination.setFileChooserCustomizer(fc -> {
            fc.setDialogTitle("Export Music...");
            fc.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("Zip Files", "zip"));
            fc.setSelectedFile(new File("export.zip"));
            fc.setAcceptAllFileFilterUsed(false);
        });

        Color mutedColor = UIManager.getColor("Label.disabledForeground");
        if (mutedColor != null) {
            destination.setForeground(mutedColor);
        }

        mainPanel.add(destination);

        mainPanel.add(Box.createVerticalStrut(20));

        JLabel formatLabel = new JLabel("Song format");
        formatLabel.setFont(formatLabel.getFont().deriveFont(Font.BOLD, 13f));
        formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(formatLabel);

        mainPanel.add(Box.createVerticalStrut(5));

        formatPicker.addItem(new FormatOption("Keep original format", null));
        for (AlbumFormatNormalizer.AudioFormat format : AlbumFormatNormalizer.allFormats()) {
            formatPicker.addItem(new FormatOption(format.toString(), format));
        }

        formatPicker.setSelectedIndex(0);
        formatPicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        formatPicker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        formatPicker.addActionListener(_ -> updateFormatDetails());
        mainPanel.add(formatPicker);

        mainPanel.add(Box.createVerticalStrut(8));

        formatDetails.setFont(formatDetails.getFont().deriveFont(Font.PLAIN, 11f));
        if (mutedColor != null) {
            formatDetails.setForeground(mutedColor);
        }
        formatDetails.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(formatDetails);

        mainPanel.add(Box.createVerticalGlue());

        JButton cancelButton = new JButton("Cancel");
        JButton startButton = new JButton("Start");

        cancelButton.addActionListener(_ -> dispose());

        startButton.addActionListener(this::onStart);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        buttonPanel.add(cancelButton);
        buttonPanel.add(startButton);

        mainPanel.add(buttonPanel);

        getRootPane().setDefaultButton(startButton);
        getRootPane().registerKeyboardAction(
                _ -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        setContentPane(mainPanel);
        updateFormatDetails();
    }

    private void updateFormatDetails() {
        AlbumFormatNormalizer.AudioFormat format = selectedFormat();

        if (format == null) {
            formatDetails.setText("Songs are copied into the archive without re-encoding.");
            return;
        }

        formatDetails.setText("Songs are converted to " + format + " while being exported. "
                + "Your library is left untouched.");
    }

    private AlbumFormatNormalizer.AudioFormat selectedFormat() {
        FormatOption option = (FormatOption) formatPicker.getSelectedItem();
        return option == null ? null : option.format;
    }

    private void onStart(ActionEvent event) {
        AlbumFormatNormalizer.AudioFormat format = selectedFormat();

        if (processingDialog.requiresFFmpeg(format)) {
            MDialog.showMessageDialog(
                    this,
                    "FFmpeg is required to convert songs to " + format + ".",
                    "FFmpeg Required",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        processingDialog.setTargetFormat(format);
        confirmed = true;
        dispose();
    }

    /**
     * @return true if the user pressed Start, false if the export was cancelled.
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}