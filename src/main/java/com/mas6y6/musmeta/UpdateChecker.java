package com.mas6y6.musmeta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mas6y6.musmeta.config.ConfigManager;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.settings.Updates;
import com.mas6y6.musmeta.ui.MainWindow;
import com.mas6y6.musmeta.ui.dialogs.base.MPromptDialog;
import com.mas6y6.musmeta.utils.VersionSource;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(UpdateChecker.class);
    private static final String GITHUB_API_URL = "https://api.github.com/repos/mas6y6/MusMeta/releases/latest";
    private static final String RELEASES_PAGE_URL = "https://github.com/mas6y6/MusMeta/releases";

    private UpdateChecker() {
    }

    public static void checkForUpdates() {
        VersionSource.Version version = VersionSource.getVersion();
        if (version.isHomebuilt()) {
            LOGGER.info("Running MusMeta in homebuilt mode. Skipping update check.");
            return;
        }

        if (Settings.UPDATES.get() == Updates.DISABLED) {
            LOGGER.info("Update checks are disabled.");
            return;
        }

        LOGGER.info("Checking for updates...");
        CompletableFuture.runAsync(UpdateChecker::fetchLatestRelease);
    }

    private static void fetchLatestRelease() {
        try {
            VersionSource.Version currentVer = VersionSource.getVersion();
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "MusMeta-UpdateChecker")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.warn("Failed to check for updates. Status: {}", response.statusCode());
                return;
            }

            JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
            String latestTag = release.has("tag_name") && !release.get("tag_name").isJsonNull()
                    ? release.get("tag_name").getAsString()
                    : null;

            if (latestTag == null || latestTag.isBlank()) {
                return;
            }

            String currentVersion = versionToString(currentVer.musmetaVersion());
            String latestVersion = versionToString(latestTag);

            if (isNewerVersion(latestVersion, currentVersion)) {
                String skipped = Settings.SKIPPED_UPDATE_VERSION.get();
                if (skipped != null && skipped.equals(latestTag)) {
                    LOGGER.info("Update {} skipped by user.", latestTag);
                    return;
                }
                SwingUtilities.invokeLater(() -> showUpdateDialog(latestTag, release));
            } else {
                LOGGER.info("MusMeta is up to date.");
            }
        } catch (Exception e) {
            LOGGER.warn("Error while checking for updates", e);
        }
    }

    private static String versionToString(String v) {
        if (v == null) {
            return "0.0.0";
        }
        String cleaned = v.trim();
        if (cleaned.startsWith("v") || cleaned.startsWith("V")) {
            cleaned = cleaned.substring(1);
        }
        // strip anything after dash/pre-release if present for simple comparison
        int dash = cleaned.indexOf('-');
        if (dash >= 0) {
            cleaned = cleaned.substring(0, dash);
        }
        return cleaned;
    }

    private static boolean isNewerVersion(String latest, String current) {
        try {
            String[] l = latest.split("\\.");
            String[] c = current.split("\\.");
            int max = Math.max(l.length, c.length);
            for (int i = 0; i < max; i++) {
                int lv = i < l.length && !l[i].isBlank() ? Integer.parseInt(l[i]) : 0;
                int cv = i < c.length && !c[i].isBlank() ? Integer.parseInt(c[i]) : 0;
                if (lv > cv) {
                    return true;
                }
                if (lv < cv) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            return !latest.equals(current);
        }
    }

    private static void showUpdateDialog(String latestTag, JsonObject release) {
        String releaseName = release.has("name") && !release.get("name").isJsonNull()
                ? release.get("name").getAsString()
                : latestTag;

        Object[] options = {"Update", "Skip this release", "Later"};
        int choice = MPromptDialog.showOptionDialog(
                MainWindow.INSTANCE,
                "<html><b>Update Available</b><br><br>" +
                        "A new version of MusMeta is available (" + releaseName + ").<br>" +
                        "Would you like to download it from the releases page?</html>",
                "Update Available",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) { // Update
            openReleasesPage();
        } else if (choice == 1) { // Skip this release
            Settings.SKIPPED_UPDATE_VERSION.set(latestTag);
            try {
                ConfigManager.getInstance().save();
            } catch (Exception e) {
                LOGGER.warn("Failed to save config after skipping update", e);
            }
            LOGGER.info("Skipped update {}", latestTag);
        }
    }

    private static void openReleasesPage() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(RELEASES_PAGE_URL));
            } else {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", RELEASES_PAGE_URL});
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to open releases page", e);
            MPromptDialog.showMessageDialog(
                    MainWindow.INSTANCE,
                    "Could not open the browser. Please visit:\n" + RELEASES_PAGE_URL,
                    "Open Releases Page",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
}