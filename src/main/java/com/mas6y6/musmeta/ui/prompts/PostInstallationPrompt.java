package com.mas6y6.musmeta.ui.prompts;

import com.mas6y6.musmeta.Main;
import com.mas6y6.musmeta.Utils;
import com.mas6y6.musmeta.config.ConfigManager;
import com.mas6y6.musmeta.settings.Theme;
import com.mas6y6.musmeta.settings.Updates;
import com.mas6y6.musmeta.ui.components.MusMetaFrame;
import com.mas6y6.musmeta.ui.dialogs.FFmpegDownloadDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PostInstallationPrompt extends MusMetaFrame {

    private static final Dimension SIZE = new Dimension(800, 600);

    private boolean completed = false;

    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);

    private final JButton backButton = new JButton("Back");
    private final JButton nextButton = new JButton("Next");

    // SETUP VALUES

    private boolean isffmpegAutoInstall = true;
    private String ffmpegBinPath = null;
    private JTextField pathField;

    public PostInstallationPrompt() {
        setSubTitle("Post Installation");

        initWindow();
        initFailsafe();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initWindow() {
        setMinimumSize(SIZE);
        setSize(SIZE);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        tabs.addTab("Welcome", welcomePage());
        tabs.addTab("Auto updates", autoCheckUpdates());
        tabs.addTab("Theme", themePage());
        tabs.addTab("FFmpeg", ffmpegInstallation());
        tabs.addTab("Completion", completionPage());

        /*
         * FlatLaf styling
         */
        tabs.putClientProperty("JTabbedPane.tabType", "card");
        tabs.putClientProperty("JTabbedPane.tabHeight", 45);
        tabs.putClientProperty("JTabbedPane.minimumTabWidth", 140);
        tabs.putClientProperty("JTabbedPane.showTabSeparators", false);
        tabs.putClientProperty("JTabbedPane.hasFullBorder", false);

        /*
         * Don't allow clicking tabs to change pages.
         */
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() != currentPage) {
                tabs.setSelectedIndex(currentPage);
            }
        });

        /*
         * Bottom navigation
         */
        JPanel navigation = new JPanel(new BorderLayout());

        navigation.setBorder(
                BorderFactory.createEmptyBorder(10, 20, 20, 20)
        );

        navigation.add(backButton, BorderLayout.WEST);
        navigation.add(nextButton, BorderLayout.EAST);

        backButton.addActionListener(e -> previousPage());
        nextButton.addActionListener(e -> nextPage());

        /*
         * Main layout
         */
        setLayout(new BorderLayout());

        add(tabs, BorderLayout.CENTER);
        add(navigation, BorderLayout.SOUTH);

        updateNavigation();
    }

    private int currentPage = 0;

    private void nextPage() {
        if (!validateCurrentPage()) {
            return;
        }

        if (currentPage >= tabs.getTabCount() - 1) {
            completeInstallation();
            return;
        }

        currentPage++;
        tabs.setSelectedIndex(currentPage);
        updateNavigation();
    }

    private void previousPage() {
        if (currentPage <= 0) {
            return;
        }

        currentPage--;

        tabs.setSelectedIndex(currentPage);

        updateNavigation();
    }

    private void updateNavigation() {
        backButton.setEnabled(currentPage > 0);

        if (currentPage == tabs.getTabCount() - 1) {
            nextButton.setText("Finish");
        } else {
            nextButton.setText("Next");
        }

        updateTabTitles();
    }

    private void updateTabTitles() {
        tabs.setTitleAt(0, currentPage > 0 ? "✓ Welcome" : "Welcome");
        tabs.setTitleAt(1, currentPage > 1 ? "✓ Auto updates" : "Auto updates");
        tabs.setTitleAt(2, currentPage > 2 ? "✓ Theme" : "Theme");
        tabs.setTitleAt(3, currentPage > 3 ? "✓ FFmpeg" : "FFmpeg");
        tabs.setTitleAt(4, currentPage > 4 ? "✓ Completion" : "Completion");
    }

    private void initFailsafe() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!completed) {
                    handleAbortInstallation();
                } else {
                    dispose();
                }
            }
        });
    }

    /**
     * Called when the user finishes all installation / setup steps.
     */
    public void completeInstallation() {
        ConfigManager.getInstance().getConfig("app").setValue("auto_ffmpeg_install", isffmpegAutoInstall);
        if (isffmpegAutoInstall) {
            ConfigManager.getInstance().getConfig("app").setValue("ffmpeg_installation_path", "");
            Path targetDir = Paths.get(Main.appDir.toString(), "bins");
            FFmpegDownloadDialog downloadDialog = new FFmpegDownloadDialog(this, targetDir);
            boolean success = downloadDialog.startAndShow();
            if (!success) {
                return;
            }
        } else {
            if (Utils.validateFFmpegExecutable(Path.of(ffmpegBinPath))) {
                ConfigManager.getInstance().getConfig("app").setValue("ffmpeg_installation_path", ffmpegBinPath);
            }
        }

        try {
            ConfigManager configManager = ConfigManager.getInstance();

            var appConfig = configManager.getConfig("app");

            if (appConfig != null) {
                appConfig.setValue("setup_completed", true);
            }

            configManager.save();

            completed = true;

            dispose();

            // Launch main application UI here
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to save configuration: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Resets and deletes incomplete config if the user closes the window prematurely.
     */
    public void handleAbortInstallation() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Setup is not complete. Are you sure you want to exit? Your changes will be reset.",
                "Exit Setup",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            resetAndCleanupConfig();

            dispose();

            System.exit(0);
        }
    }

    public void resetAndCleanupConfig() {
        try {
            ConfigManager configManager = ConfigManager.getInstance();

            configManager.resetAll();

            Path configPath = configManager.getConfigPath();

            Files.deleteIfExists(configPath);

        } catch (IOException ex) {
            System.err.println(
                    "Failed to delete config during reset: "
                            + ex.getMessage()
            );
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    private boolean validateCurrentPage() {
        return switch (currentPage) {
            case 4 -> validateFFmpegPage();
            default -> true;
        };
    }

    public JPanel welcomePage() {
        JPanel page = new JPanel(new BorderLayout(20, 20));
        page.setBorder(BorderFactory.createEmptyBorder(
                30, 40, 30, 40
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Welcome to MusMeta");
        title.setFont(title.getFont().deriveFont(Font.PLAIN, 28f));

        JLabel description = new JLabel("""
<html>
    Before you can start using MusMeta, we need to configure a few things.
    
    To continue, please press next.
</html>""");

        content.add(title);
        content.add(Box.createVerticalStrut(10));
        content.add(description);
        page.add(content, BorderLayout.NORTH);

        return page;
    }

    private JPanel completionPage() {
        JPanel page = new JPanel(new BorderLayout(20, 20));
        page.setBorder(BorderFactory.createEmptyBorder(
                30, 40, 30, 40
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Installation Completed!");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

        JLabel description = new JLabel("""
<html>
    MusMeta has been configured successfully.<br>
    Click Finish to save your settings and start using the application.
</html>""");

        content.add(title);
        content.add(Box.createVerticalStrut(10));
        content.add(description);
        page.add(content, BorderLayout.NORTH);

        return page;
    }

    public JPanel autoCheckUpdates() {
        JPanel page = new JPanel(new BorderLayout(20, 20));
        page.setBorder(BorderFactory.createEmptyBorder(
                30, 40, 30, 40
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Enable Automatic Updates?");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

        JLabel description = new JLabel("""
<html>
    Automatic updates help ensure that you always have the latest version of MusMeta.<br>
    You can disable this feature at any time.
</html>
""");

        content.add(title);
        content.add(Box.createVerticalStrut(10));
        content.add(description);
        content.add(Box.createVerticalStrut(10));

        JRadioButton enableUpdatesBtn =
                new JRadioButton("Enable auto updates");

        JRadioButton promptOnlyUpdateBtn =
                new JRadioButton("Disable auto updates, but still prompt for updates.");

        JRadioButton disableUpdatesBtn =
                new JRadioButton("Disable auto updates");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(enableUpdatesBtn);
        buttonGroup.add(promptOnlyUpdateBtn);
        buttonGroup.add(disableUpdatesBtn);

        enableUpdatesBtn.addActionListener(
                e -> {
                    ConfigManager.getInstance().getConfig("app").setValue("updates", Updates.ENABLED);
                }
        );

        promptOnlyUpdateBtn.addActionListener(
                e -> {
                    ConfigManager.getInstance().getConfig("app").setValue("updates", Updates.PROMPT_ONLY);
                }
        );

        disableUpdatesBtn.addActionListener(
                e -> {
                    ConfigManager.getInstance().getConfig("app").setValue("updates", Updates.DISABLED);
                }
        );

        enableUpdatesBtn.setSelected(true);

        content.add(enableUpdatesBtn);
        content.add(Box.createVerticalStrut(8));
        content.add(promptOnlyUpdateBtn);
        content.add(Box.createVerticalStrut(8));
        content.add(disableUpdatesBtn);

        page.add(content, BorderLayout.NORTH);

        return page;
    }

    public JPanel themePage() {
        JPanel page = new JPanel(new BorderLayout(20, 20));
        page.setBorder(BorderFactory.createEmptyBorder(
                30, 40, 30, 40
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Choose a Theme");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

        JLabel description = new JLabel("""
<html>
    Light mode and dark mode themes are available. You can change the theme at any time.
</html>
""");

        content.add(title);
        content.add(Box.createVerticalStrut(10));
        content.add(description);
        content.add(Box.createVerticalStrut(10));

        JRadioButton systemDefaultBtn =
                new JRadioButton("Automatic with system");

        JRadioButton lightBtn =
                new JRadioButton("Light");

        JRadioButton darkBtn =
                new JRadioButton("Dark");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(systemDefaultBtn);
        buttonGroup.add(lightBtn);
        buttonGroup.add(darkBtn);


        systemDefaultBtn.addActionListener(
                e -> {
                    ConfigManager.getInstance().getConfig("app").setValue("preferred_theme", Theme.SYSTEM);
                }
        );

        lightBtn.addActionListener(
                e -> {
                    ConfigManager.getInstance().getConfig("app").setValue("preferred_theme", Theme.LIGHT);
                }
        );

        darkBtn.addActionListener(
                e -> {
                    ConfigManager.getInstance().getConfig("app").setValue("preferred_theme", Theme.DARK);
                }
        );

        systemDefaultBtn.setSelected(true);

        content.add(systemDefaultBtn);
        content.add(Box.createVerticalStrut(8));
        content.add(lightBtn);
        content.add(Box.createVerticalStrut(8));
        content.add(darkBtn);

        page.add(content, BorderLayout.NORTH);

        return page;
    }


    /*
        FFMPEG
    */
    public JPanel ffmpegInstallation() {
        JPanel page = new JPanel(new BorderLayout(20, 20));
        page.setBorder(BorderFactory.createEmptyBorder(
                30, 40, 30, 40
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("FFmpeg Installation");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

        JLabel description = new JLabel("""
        <html>
            FFmpeg is required to process audio files. Depending on your setup,
            you can let MusMeta install it for you.<br>
            Or you can use an existing installation.
        </html>""");

        content.add(title);
        content.add(Box.createVerticalStrut(10));
        content.add(description);
        content.add(Box.createVerticalStrut(25));

        // Radio buttons
        JRadioButton installRadio =
                new JRadioButton("Install FFmpeg automatically");

        JRadioButton existingRadio =
                new JRadioButton("Use an existing FFmpeg installation");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(installRadio);
        buttonGroup.add(existingRadio);

        installRadio.setSelected(true);

        content.add(installRadio);
        content.add(Box.createVerticalStrut(8));
        content.add(existingRadio);

        // Existing installation controls
        JPanel existingPanel = new JPanel(new BorderLayout(10, 0));

        pathField = new JTextField();
        JButton browseButton = new JButton("Browse...");

        existingPanel.add(pathField, BorderLayout.CENTER);
        existingPanel.add(browseButton, BorderLayout.EAST);

        // Give it a little indentation so it belongs to the radio option
        existingPanel.setBorder(
                BorderFactory.createEmptyBorder(5, 25, 5, 0)
        );

        existingPanel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40)
        );

        content.add(existingPanel);

        // Hidden initially
        existingPanel.setVisible(false);

        // Show/hide when radio button changes
        existingRadio.addActionListener(e -> {
            isffmpegAutoInstall = false;
            existingPanel.setVisible(true);
            content.revalidate();
            content.repaint();
        });

        installRadio.addActionListener(e -> {
            isffmpegAutoInstall = true;
            existingPanel.setVisible(false);
            content.revalidate();
            content.repaint();
        });

        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        installRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        existingRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        existingPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Browse button
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle("Select FFmpeg bin directory");

            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            int result = chooser.showOpenDialog(page);

            if (result == JFileChooser.APPROVE_OPTION) {
                String selectedPath = chooser.getSelectedFile().getAbsolutePath();
                pathField.setText(selectedPath);
                ffmpegBinPath = selectedPath;
            }
        });

        page.add(content, BorderLayout.NORTH);

        return page;
    }

    private boolean validateFFmpegPage() {
        if (isffmpegAutoInstall) {
            return true;
        }

        String rawPath = pathField != null && pathField.getText() != null
                ? pathField.getText().trim()
                : (ffmpegBinPath != null ? ffmpegBinPath.trim() : "");

        if (rawPath.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select or enter the path to the FFmpeg bin directory.",
                    "FFmpeg Validation",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        Path binDir = Paths.get(rawPath);
        if (!Files.exists(binDir)) {
            JOptionPane.showMessageDialog(
                    this,
                    "The selected FFmpeg bin directory does not exist.",
                    "FFmpeg Validation",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        Path executable = Utils.findFFmpegExecutable(binDir);
        if (executable == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "FFmpeg executable not found in the selected directory.\nPlease ensure the directory contains the 'ffmpeg' binary.",
                    "FFmpeg Validation",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        if (!Utils.validateFFmpegExecutable(executable)) {
            JOptionPane.showMessageDialog(
                    this,
                    "The FFmpeg executable in the selected directory is invalid or failed to run.",
                    "FFmpeg Validation",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        ffmpegBinPath = binDir.toAbsolutePath().toString();
        return true;
    }

    /*
        FFMPEG
    */
}