package com.mas6y6.musmeta.core;

import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.settings.Settings;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class CoreUtils {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CoreUtils.class);

    public static void scanForMusicFiles(Path... ignorePaths) {
        LOGGER.info("Scanning for music files...");
        LOGGER.info("Ignoring paths: {}", Arrays.toString(ignorePaths));

        Set<Path> ignored = Arrays.stream(ignorePaths)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .collect(Collectors.toUnmodifiableSet());

        Path musicDirectory = Settings.MUSIC_DIRECTORY_PATH.get()
                .toAbsolutePath()
                .normalize();

        ArrayList<Song> musicFiles = new ArrayList<>();

        try {
            Files.walkFileTree(musicDirectory, new SimpleFileVisitor<>() {

                @Override
                public @NonNull FileVisitResult preVisitDirectory(
                        @NonNull Path dir,
                        @NonNull BasicFileAttributes attrs
                ) {
                    if (ignored.contains(dir.toAbsolutePath().normalize())) {
                        LOGGER.info("Skipping directory: {}", dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFile(
                        @NonNull Path file,
                        @NonNull BasicFileAttributes attrs
                ) {
                    if (!attrs.isRegularFile()) {
                        return FileVisitResult.CONTINUE;
                    }

                    String extension = com.google.common.io.Files.getFileExtension(file.toString()).toLowerCase(Locale.ROOT);
                    try {
                        if (Constants.MUSIC_EXTENSIONS.contains(extension)) {
                            AudioFile audioFile = AudioFileIO.read(file.toFile());

                            musicFiles.add(new Song(audioFile));

                            LOGGER.info("Processing file: {}", file);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error processing file: {}", file, e);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFileFailed(
                        @NonNull Path file,
                        @NonNull IOException exc
                ) {
                    LOGGER.error("{}{}", "Skipping unreadable item: "
                            + file
                            + " due to ", exc.getMessage());

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan music directory", e);
        }
    }
}
