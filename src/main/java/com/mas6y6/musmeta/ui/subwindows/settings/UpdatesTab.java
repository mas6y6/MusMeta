package com.mas6y6.musmeta.ui.subwindows.settings;

import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.settings.Updates;

import javax.swing.*;
import java.awt.*;

public class UpdatesTab extends JPanel {
    private final JRadioButton promptOnlyRadio =
            new JRadioButton("Prompt Only");
    private final JRadioButton disabledRadio =
            new JRadioButton("Disabled");

    public UpdatesTab() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Updates");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);

        content.add(Box.createVerticalStrut(5));

        JLabel description = new JLabel(
                "Configure how the application checks for updates."
        );
        description.setForeground(UIManager.getColor("Label.disabledForeground"));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(description);

        content.add(Box.createVerticalStrut(12));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(promptOnlyRadio);
        buttonGroup.add(disabledRadio);

        content.add(promptOnlyRadio);
        content.add(disabledRadio);

        promptOnlyRadio.addActionListener(e -> {
            Settings.UPDATES.set(Updates.PROMPT_ONLY);
        });
        disabledRadio.addActionListener(e -> {
            Settings.UPDATES.set(Updates.DISABLED);
        });

        configureInitState();

        add(content, BorderLayout.CENTER);
    }

    public void configureInitState() {
        if (Settings.UPDATES.get() == Updates.PROMPT_ONLY) {
            promptOnlyRadio.setSelected(true);
            disabledRadio.setSelected(false);
        } else if (Settings.UPDATES.get() == Updates.DISABLED) {
            promptOnlyRadio.setSelected(false);
            disabledRadio.setSelected(true);
        }
    }
}
