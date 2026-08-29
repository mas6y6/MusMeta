package com.mas6y6.musmeta.ui.prompts;

import javax.swing.*;
import java.awt.*;

public class MusicScanPrompt extends JDialog {
    private static final Dimension DIALOG_SIZE = new Dimension(480, 500);

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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel("Scan Music Library");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);



        setContentPane(mainPanel);
    }
}
