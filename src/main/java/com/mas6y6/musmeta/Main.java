package com.mas6y6.musmeta;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.Gson;
import com.mas6y6.musmeta.ui.prompts.PostInstallationPrompt;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static Gson GSON = new Gson();

    public static void main(String[] args) {
        System.out.println("com.mas6y6.musmeta.Main.main()");

        if (!Files.exists(Paths.get(System.getProperty("user.home"), ".musmeta"))) {
            if (!Paths.get(System.getProperty("user.home"), ".musmeta").toFile().mkdirs()) {
                throw new RuntimeException("Failed to create .musmeta directory");
            }
        }

        FlatLightLaf.setup();
        JFrame.setDefaultLookAndFeelDecorated(true);
        SwingUtilities.invokeLater(() -> {
            new PostInstallationPrompt().addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    // Window has been closed
                }
            });
        });
    }
}
