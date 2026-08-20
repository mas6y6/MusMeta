package com.mas6y6.musmeta;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.Gson;

import javax.swing.*;
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
    }
}
