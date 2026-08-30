package com.mas6y6.musmeta.core;

import com.mas6y6.musmeta.Constants;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class Core {
    static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Core.class);

    public record ScanResult(List<Song> musicFiles, List<UntaggedSong> untaggedSongs, List<Album> albums) {}

    public static List<Album> getAlbums() {
        return Library.getInstance().getAlbums();
    }

    public static List<Song> getLibrary() {
        return Library.getInstance().getSongs();
    }

    public static void addSong(Song song) {
        Library.getInstance().addSong(song);
    }

    public static void reset() {
        Library.getInstance().clear();
    }

    /**
     * Scans a directory tree for music, rebuilds the library from the tagged
     * songs found, and persists it.
     */
    public static ScanResult scanForMusicFiles(Path musicDir, List<Path> ignorePaths) {
        LOGGER.info("Scanning for music files...");
        LOGGER.info("Ignoring paths: {}", ignorePaths);

        Set<Path> ignored = ignorePaths.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .collect(Collectors.toUnmodifiableSet());

        Path musicDirectory = musicDir
                .toAbsolutePath()
                .normalize();

        ArrayList<Song> musicFiles = new ArrayList<>();
        ArrayList<UntaggedSong> untaggedSongs = new ArrayList<>();

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

                            if (audioFile.getTag() == null) {
                                untaggedSongs.add(new UntaggedSong(audioFile));
                            } else {
                                musicFiles.add(new Song(audioFile));
                            }

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

        Library library = Library.getInstance();
        library.clear();
        for (Song song : musicFiles) {
            library.addSong(song);
        }
        library.save();

        return new ScanResult(List.copyOf(musicFiles), List.copyOf(untaggedSongs), library.getAlbums());
    }
}