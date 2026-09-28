package com.mas6y6.musmeta;

import com.mas6y6.musmeta.utils.VersionSource;
import org.slf4j.Logger;

public class UpdateChecker {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(UpdateChecker.class);

    private UpdateChecker() {
    }

    public static void checkForUpdates() {
        if (VersionSource.getVersion().isHomebuilt()) {
            LOGGER.info("Running MusMeta in homebuilt mode. Skipping update check.");
        } else {
            LOGGER.info("Checking for updates...");


        }
    }
}
