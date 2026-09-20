package com.mas6y6.musmeta.core;

import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.Main;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotReadVideoException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Core {
    static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Core.class);

    public record ScanResult(List<Song> musicFiles, List<Album> albums) {}

    /**
     * Decides what to do with duplicate songs discovered during a scan.
     * Implementations should present the choice UI and block until the user
     * has made a decision.
     */
    @FunctionalInterface
    public interface DuplicateResolver {

        /**
         * Called with every duplicate group found while scanning.
         *
         * @return a choice per group ({@code null} value: import nothing for
         *         that group and keep the existing songs), or {@code null} to
         *         abort the scan without touching the library.
         */
        Map<Duplicates.Group, Song> resolve(List<Duplicates.Group> groups);
    }

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

    public static ScanResult scanForMusicFiles(Path musicDir, List<Path> ignorePaths) {
        return scanForMusicFiles(musicDir, ignorePaths, null);
    }

    /**
     * Scans a directory tree for music, incrementally syncing the library:
     * songs already in the library are kept, newly discovered songs are added,
     * and songs whose files no longer exist on disk are removed. Persists only
     * when the library actually changed.
     *
     * <p>When {@code resolver} is provided, duplicate songs (same track found
     * in another file, folder or format) are reported to it so the user can
     * pick which file to import. If the resolver returns {@code null}, the
     * scan is aborted without modifying the library.
     */
    public static ScanResult scanForMusicFiles(Path musicDir, List<Path> ignorePaths, DuplicateResolver resolver) {
        LOGGER.info("Scanning for music files...");
        LOGGER.info("Ignoring paths: {}", ignorePaths);

        Set<Path> ignored = ignorePaths.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .collect(Collectors.toUnmodifiableSet());

        Path musicDirectory = musicDir
                .toAbsolutePath()
                .normalize();

        Path musMetaDirectory = Main.musMetaDirectory;

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

                    if (dir.toAbsolutePath().normalize().equals(musMetaDirectory)) {
                        LOGGER.info("Skipping already-converted MusMeta directory: {}", dir);
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
                    if (Constants.MUSIC_EXTENSIONS.contains(extension)) {
                        try {
                            AudioFile audioFile = AudioFileIO.read(file.toFile());
                            Song song = new Song(audioFile);
                            musicFiles.add(song);

                            LOGGER.info("Processing file: {}", file);
                        } catch (CannotReadVideoException e) {
                            // Container holds a video track (e.g. MP4/M4A/OGG video) -> not audio-only.
                            LOGGER.warn("Skipping video file (not audio-only): {}", file);
                        } catch (CannotReadException e) {
                            // File could not be parsed as audio (e.g. broken or video-only container).
                            LOGGER.warn("Skipping file that cannot be read as audio: {} ({})", file, e.getMessage());
                        } catch (IOException | TagException | ReadOnlyFileException |
                                 InvalidAudioFrameException e) {
                            LOGGER.error("Error processing file: {}", file, e);
                        }
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
                            + " due to ", exc);

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan music directory", e);
        }

        Library library = Library.getInstance();

        Map<Path, Song> existingByPath = new HashMap<>();
        for (Song song : library.getSongs()) {
            existingByPath.put(
                    song.getAudioFile().getFile().toPath().toAbsolutePath().normalize(),
                    song
            );
        }

        List<Duplicates.Group> groups = List.of();
        Map<Duplicates.Group, Song> choices = Map.of();
        if (resolver != null) {
            groups = Duplicates.findGroups(musicFiles, library.getSongs());
            if (!groups.isEmpty()) {
                choices = resolver.resolve(groups);
                if (choices == null) {
                    LOGGER.info("Scan cancelled during duplicate resolution; library left unchanged");
                    return new ScanResult(List.copyOf(musicFiles), library.getAlbums());
                }
            }
        }

        Map<Song, Duplicates.Group> incomingToGroup = new IdentityHashMap<>();
        for (Duplicates.Group group : groups) {
            for (Song incoming : group.incoming()) {
                incomingToGroup.put(incoming, group);
            }
        }

        int removed = 0;
        for (Map.Entry<Path, Song> entry : existingByPath.entrySet()) {
            if (!Files.exists(entry.getKey())) {
                library.removeSong(entry.getValue());
                removed++;
            }
        }

        List<Song> toImport = new ArrayList<>();
        Set<Duplicates.Group> handled = Collections.newSetFromMap(new IdentityHashMap<>());
        int replaced = 0;

        for (Song song : musicFiles) {
            Path path = song.getAudioFile().getFile().toPath().toAbsolutePath().normalize();
            if (existingByPath.containsKey(path)) {
                continue;
            }

            Duplicates.Group group = incomingToGroup.get(song);
            if (group == null) {
                toImport.add(song);
                continue;
            }
            if (!handled.add(group)) {
                continue;
            }

            Song chosen = choices.get(group);
            if (chosen == null) {
                continue;
            }

            if (group.incoming().contains(chosen)) {
                for (Song existing : group.existing()) {
                    library.removeSong(existing);
                    replaced++;
                }
                toImport.add(chosen);
            }
        }

        int added = 0;
        for (Song song : toImport) {
            library.addSong(song);
            added++;
        }

        if (added > 0 || removed > 0 || replaced > 0) {
            library.save();
        }

        LOGGER.info("Scan complete: {} new song(s), {} replaced, {} removed", added, replaced, removed);

        return new ScanResult(List.copyOf(musicFiles), library.getAlbums());
    }
}