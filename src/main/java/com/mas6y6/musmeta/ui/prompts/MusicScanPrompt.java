package com.mas6y6.musmeta.ui.prompts;

import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.dialogs.MusicScanDialog;

import javax.swing.*;
import java.awt.*;

public class MusicScanPrompt extends JDialog {
    private static final Dimension DIALOG_SIZE = new Dimension(480, 500);
    private boolean useDefaultMusicDir = false;
    private JTextField pathField = new JTextField();

    public MusicScanPrompt(JFrame parentWindow) {
        super(parentWindow, "Music Scan", true);
        setSize(DIALOG_SIZE);
        setMinimumSize(DIALOG_SIZE);
        setResizable(false);
        setLocationRelativeTo(getOwner());
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        );

        JLabel titleLabel = new JLabel("Music Scan");
        titleLabel.setFont(
                titleLabel.getFont().deriveFont(Font.BOLD, 18f)
        );
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);

        JLabel scanLabel = new JLabel("Select the directory to scan:");
        scanLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(scanLabel);

        JRadioButton useDefaultMusicDir =
                new JRadioButton(
                        "Use " + Settings.MUSIC_DIRECTORY_PATH.get()
                );

        JRadioButton useCustomDir =
                new JRadioButton("Custom directory");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(useDefaultMusicDir);
        buttonGroup.add(useCustomDir);

        useDefaultMusicDir.setSelected(true);

        useDefaultMusicDir.setAlignmentX(Component.LEFT_ALIGNMENT);
        useCustomDir.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(useDefaultMusicDir);

        mainPanel.add(Box.createVerticalStrut(3));
        mainPanel.add(useCustomDir);

        JPanel customDirPanel = customDirPanel();
        customDirPanel.setVisible(false);
        customDirPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainPanel.add(customDirPanel);

        useDefaultMusicDir.addActionListener(e -> {
            this.useDefaultMusicDir = true;
            customDirPanel.setVisible(false);
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        useCustomDir.addActionListener(e -> {
            this.useDefaultMusicDir = false;
            customDirPanel.setVisible(true);
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        setContentPane(mainPanel);

        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(buttons());
    }

    private JPanel customDirPanel() {
        var panel = new JPanel(new BorderLayout(10, 0));

        pathField.setText(Settings.MUSIC_DIRECTORY_PATH.get().toString());

        var browseButton = new JButton("Browse...");

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle("Select music directory");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            int result = chooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                pathField.setText(
                        chooser.getSelectedFile().getAbsolutePath()
                );
            }
        });

        panel.add(pathField, BorderLayout.CENTER);
        panel.add(browseButton, BorderLayout.EAST);

        panel.setBorder(
                BorderFactory.createEmptyBorder(5, 25, 5, 0)
        );

        panel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40)
        );

        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        return panel;
    }

    private JPanel buttons() {
        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0)
        );

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            this.dispose();
        });

        JButton scanButton = new JButton("Scan");
        scanButton.addActionListener(e -> {
            var musicScanDialog = new MusicScanDialog(this,useDefaultMusicDir,pathField.getText());
            musicScanDialog.startAndShow();
            this.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(scanButton);

        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        buttonPanel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, buttonPanel.getPreferredSize().height)
        );

        return buttonPanel;
    }
}
