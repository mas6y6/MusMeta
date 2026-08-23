package com.mas6y6.musmeta.utils;

import java.nio.charset.StandardCharsets;

public class Utils {


    public static boolean isRunningAsRoot() {
        String user = System.getProperty("user.name");
        if ("root".equalsIgnoreCase(user)) {
            return true;
        }
        String uid = System.getenv("UID");
        if ("0".equals(uid)) {
            return true;
        }
        try {
            Process process = new ProcessBuilder("id", "-u")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.waitFor() == 0 && "0".equals(output)) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
