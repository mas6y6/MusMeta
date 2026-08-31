package com.mas6y6.musmeta.utils;

import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Disc;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.dialogs.base.EXTDialog;
import org.jaudiotagger.audio.AudioFileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prompts for and normalizes mixed audio formats within scanned albums.
 * Source files are only removed after FFmpeg has successfully written their
 * replacement, and an existing target file is never overwritten.
 */
public final class AlbumFormatNormalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlbumFormatNormalizer.class);

    private AlbumFormatNormalizer() {
    }

    public enum AudioFormat {
        FLAC("flac", "flac", List.of()),
        MP3("mp3", "libmp3lame", List.of("-q:a", "0")),
        M4A("m4a", "aac", List.of("-b:a", "256k")),
        OGG("ogg", "libvorbis", List.of("-q:a", "6"));

        private final String extension;
        private final String codec;
        private final List<String> codecArguments;

        AudioFormat(String extension, String codec, List<String> codecArguments) {
            this.extension = extension;
            this.codec = codec;
            this.codecArguments = codecArguments;
        }

        public String extension() {
            return extension;
        }

        private String codec() {
            return codec;
        }

        private List<String> codecArguments() {
            return codecArguments;
        }

        @Override
        public String toString() {
            return extension.toUpperCase(Locale.ROOT);
        }
    }

    public record ConversionResult(int converted, List<Path> failedFiles) {
        public boolean succeeded() {
            return failedFiles.isEmpty();
        }
    }

    /**
     * Prompts once per mixed-format album. Choosing "Convert all remaining"
     * applies the selected format to every later mixed-format album.
     */
    public static void promptToNormalizeMixedAlbums(Component parent, Collection<Album> albums) {
        Path ffmpeg = FFmpegUtils.getFFmpegExecutable();
        if (ffmpeg == null) {
            EXTDialog.showMessageDialog(parent,
                    "FFmpeg is required to convert album audio formats.",
                    "FFmpeg Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AudioFormat applyToAllFormat = null;
        for (Album album : albums) {
            Set<String> formats = extensionsIn(album);
            if (formats.size() < 2) {
                continue;
            }

            AudioFormat target = applyToAllFormat;
            if (target == null) {
                target = promptForFormat(parent, album, formats);
                if (target == null) {
                    continue;
                }

                int action = EXTDialog.showOptionDialog(parent,
                        "Convert the tracks in \"" + album.getTitle() + "\" to " + target
                                + "?\nOriginal files are replaced only after a successful conversion.",
                        "Normalize Album Format",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                        new Object[]{"Convert album", "Convert all remaining", "Skip album"},
                        "Convert album");
                if (action == 2 || action == JOptionPane.CLOSED_OPTION) {
                    continue;
                }
                if (action == 1) {
                    applyToAllFormat = target;
                }
            }

            ConversionResult result = normalize(album, target, ffmpeg);
            if (!result.succeeded()) {
                EXTDialog.showMessageDialog(parent,
                        "Converted " + result.converted() + " track(s) in \"" + album.getTitle()
                                + "\". " + result.failedFiles().size() + " track(s) could not be converted.",
                        "Some Tracks Were Not Converted", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Converts every track in {@code album} that is not already in
     * {@code targetFormat}. Callers can use this without displaying a prompt.
     */
    public static ConversionResult normalize(Album album, AudioFormat targetFormat) {
        Path ffmpeg = FFmpegUtils.getFFmpegExecutable();
        if (ffmpeg == null) {
            throw new IllegalStateException("FFmpeg is required to convert album audio formats.");
        }
        return normalize(album, targetFormat, ffmpeg);
    }

    /**
     * Resolves an {@link AudioFormat} from a stored setting value (its extension,
     * e.g. {@code "flac"}). Unknown or blank values fall back to FLAC.
     */
    public static AudioFormat fromSetting(String extension) {
        String normalized = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
        for (AudioFormat format : AudioFormat.values()) {
            if (format.extension().equals(normalized)) {
                return format;
            }
        }
        return AudioFormat.FLAC;
    }

    /** All supported convertable audio formats, used by settings UI. */
    public static List<AudioFormat> allFormats() {
        return List.of(AudioFormat.values());
    }


    @FunctionalInterface
    public interface ConversionProgress {
        void update(int completed, int total, String details);
    }

    /**
     * Converts every song that is not already in {@code targetFormat} into the
     * {@code outputDir} subfolder, mirroring each song's path relative to
     * {@code musicRoot}. The converted file is written to the output folder and
     * the library's {@code Song} is re-pointed to it; the original source file is
     * left untouched. A song whose converted copy already exists in the output
     * folder is skipped, so re-scans don't convert the same file twice.
     * Songs already in the target format are left untouched.
     *
     * @return the number of songs converted
     */
    public static int convertIncompatibleToFolder(
            Collection<Song> songs,
            AudioFormat targetFormat,
            Path musicRoot,
            Path outputDir
    ) {
        return convertIncompatibleToFolder(songs, targetFormat, musicRoot, outputDir, null);
    }

    /**
     * Like {@link #convertIncompatibleToFolder(Collection, AudioFormat, Path, Path)}
     * but reports progress through {@code progress} after each file is processed.
     */
    public static int convertIncompatibleToFolder(
            Collection<Song> songs,
            AudioFormat targetFormat,
            Path musicRoot,
            Path outputDir,
            ConversionProgress progress
    ) {
        if (targetFormat == null || songs.isEmpty()) {
            return 0;
        }

        Path ffmpeg = FFmpegUtils.getFFmpegExecutable();
        if (ffmpeg == null) {
            return 0;
        }

        Path music = musicRoot.toAbsolutePath().normalize();

        List<Song> candidates = new ArrayList<>();
        for (Song song : songs) {
            Path source = song.getAudioFile().getFile().toPath();
            if (extensionOf(source).equals(targetFormat.extension())) {
                continue;
            }
            Path absoluteSource = source.toAbsolutePath().normalize();
            if (!absoluteSource.startsWith(music)) {
                continue;
            }
            // Skip songs that already have a converted copy in the output folder,
            // so re-scans don't convert the same original twice.
            Path relative = music.relativize(absoluteSource);
            if (Files.exists(resolveTarget(outputDir, relative, targetFormat))) {
                continue;
            }
            candidates.add(song);
        }

        int total = candidates.size();
        int threads = Math.max(1, Settings.FFMPEG_CONVERSION_THREADS.get());

        if (threads == 1 || total <= 1) {
            return convertSequentially(candidates, music, outputDir, targetFormat, ffmpeg, progress);
        }

        ExecutorService pool = Executors.newFixedThreadPool(
                Math.min(threads, total)
        );
        AtomicInteger completed = new AtomicInteger();
        AtomicInteger converted = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (Song song : candidates) {
            futures.add(pool.submit(() -> {
                Path source = song.getAudioFile().getFile().toPath();
                Path relative = music.relativize(source.toAbsolutePath().normalize());
                Path target = resolveTarget(outputDir, relative, targetFormat);
                try {
                    convertTo(song, source, target, targetFormat, ffmpeg);
                    converted.incrementAndGet();
                } catch (Exception exception) {
                    LOGGER.error("Could not convert {} to {}", source, target, exception);
                }
                int done = completed.incrementAndGet();
                if (progress != null) {
                    progress.update(done, total, source.getFileName().toString());
                }
                return null;
            }));
        }

        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return converted.get();
    }

    private static int convertSequentially(
            Collection<Song> candidates,
            Path music,
            Path outputDir,
            AudioFormat targetFormat,
            Path ffmpeg,
            ConversionProgress progress
    ) {
        int total = candidates.size();
        int converted = 0;
        int completed = 0;
        for (Song song : candidates) {
            Path source = song.getAudioFile().getFile().toPath();
            Path relative = music.relativize(source.toAbsolutePath().normalize());
            Path target = resolveTarget(outputDir, relative, targetFormat);
            try {
                convertTo(song, source, target, targetFormat, ffmpeg);
                converted++;
            } catch (Exception exception) {
                LOGGER.error("Could not convert {} to {}", source, target, exception);
            }
            completed++;
            if (progress != null) {
                progress.update(completed, total, source.getFileName().toString());
            }
        }
        return converted;
    }

    private static Path resolveTarget(Path outputDir, Path relative, AudioFormat format) {
        Path parent = relative.getParent();
        String fileName = relative.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');
        String baseName = extensionStart > 0 ? fileName.substring(0, extensionStart) : fileName;
        String targetName = (baseName.isBlank() ? "track" : baseName) + "." + format.extension();
        Path target = parent == null ? Path.of(targetName) : parent.resolve(targetName);
        return outputDir.resolve(target);
    }

    private static void convertTo(Song song, Path source, Path target, AudioFormat format, Path ffmpeg)
            throws Exception {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "musmeta-", "." + format.extension());
        Process process = null;
        try {
            List<String> command = new ArrayList<>(List.of(
                    ffmpeg.toString(), "-nostdin", "-y", "-v", "error", "-i", source.toString(),
                    "-map_metadata", "0", "-vn", "-c:a", format.codec()));
            command.addAll(format.codecArguments());
            command.add(temporary.toString());

            Process child = new ProcessBuilder(command).redirectErrorStream(true).start();
            process = child;

            // Drain output on a background thread so waitFor() below is the only
            // interruptible blocking point (lets cancel actually kill ffmpeg).
            StringBuilder output = new StringBuilder();
            Thread reader = new Thread(() -> {
                try (var in = child.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        output.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                    }
                } catch (IOException ignored) {
                    // Stream closed when the process is destroyed.
                }
            }, "ffmpeg-output-reader");
            reader.setDaemon(true);
            reader.start();

            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                destroyProcess(process);
                Thread.currentThread().interrupt();
                throw e;
            }

            if (exitCode != 0) {
                throw new IOException("FFmpeg failed (exit code " + exitCode + "): " + output);
            }

            AudioFileIO.read(temporary.toFile());

            if (Files.exists(target)) {
                deleteWithRetry(target);
            }
            moveWithRetry(temporary, target);

            var convertedAudioFile = AudioFileIO.read(target.toFile());
            song.replaceAudioFile(convertedAudioFile);
        } finally {
            destroyProcess(process);
            deleteIfExistsWithRetry(temporary);
        }
    }

    private static final long LOCK_RETRY_DELAY_MS = 25;
    private static final int LOCK_RETRY_ATTEMPTS = 40;

    /**
     * Deletes {@code file}, retrying briefly if it is transiently locked by
     * another process (e.g. an FFmpeg handle not yet released on Windows).
     */
    private static void deleteWithRetry(Path file) throws IOException {
        int attempts = 0;
        while (true) {
            try {
                Files.delete(file);
                return;
            } catch (IOException e) {
                if (++attempts >= LOCK_RETRY_ATTEMPTS) {
                    throw e;
                }
                sleepQuietly(LOCK_RETRY_DELAY_MS);
            }
        }
    }

    /**
     * Deletes {@code file} if it exists, retrying on transient locks.
     */
    private static void deleteIfExistsWithRetry(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        try {
            deleteWithRetry(file);
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }

    /**
     * Atomically moves {@code from} to {@code to}, retrying briefly if the source
     * is transiently locked.
     */
    private static void moveWithRetry(Path from, Path to) throws IOException {
        int attempts = 0;
        while (true) {
            try {
                Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
                return;
            } catch (IOException e) {
                if (++attempts >= LOCK_RETRY_ATTEMPTS) {
                    throw e;
                }
                sleepQuietly(LOCK_RETRY_DELAY_MS);
            }
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void destroyProcess(Process process) {
        if (process == null) {
            return;
        }
        if (!process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private static AudioFormat promptForFormat(Component parent, Album album, Set<String> formats) {
        AudioFormat recommended = recommendedFormat(album, formats);
        JComboBox<AudioFormat> formatPicker = new JComboBox<>(AudioFormat.values());
        formatPicker.setSelectedItem(recommended);

        Object[] message = {
                "\"" + album.getTitle() + "\" contains " + formatDescription(formats) + ".",
                "Choose one format for its tracks:",
                formatPicker
        };
        int choice = EXTDialog.showConfirmDialog(parent, message, "Mixed Album Formats",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.OK_OPTION ? (AudioFormat) formatPicker.getSelectedItem() : null;
    }

    private static ConversionResult normalize(Album album, AudioFormat targetFormat, Path ffmpeg) {
        int converted = 0;
        List<Path> failures = new ArrayList<>();
        for (Song song : songsIn(album)) {
            Path source = song.getAudioFile().getFile().toPath();
            if (extensionOf(source).equals(targetFormat.extension())) {
                continue;
            }

            try {
                convert(song, source, targetFormat, ffmpeg);
                converted++;
            } catch (Exception exception) {
                LOGGER.error("Could not convert {} to {}", source, targetFormat, exception);
                failures.add(source);
            }
        }
        return new ConversionResult(converted, List.copyOf(failures));
    }

    private static void convert(Song song, Path source, AudioFormat targetFormat, Path ffmpeg)
            throws Exception {
        String fileName = source.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');
        String baseName = extensionStart > 0 ? fileName.substring(0, extensionStart) : fileName;
        Path target = source.resolveSibling(baseName + "." + targetFormat.extension());
        if (Files.exists(target)) {
            throw new IOException("Refusing to overwrite existing file: " + target);
        }

        Path temporary = Files.createTempFile(source.getParent(), baseName + ".musmeta-", "." + targetFormat.extension());
        try {
            List<String> command = new ArrayList<>(List.of(
                    ffmpeg.toString(), "-nostdin", "-y", "-v", "error", "-i", source.toString(),
                    "-map_metadata", "0", "-vn", "-c:a", targetFormat.codec()));
            command.addAll(targetFormat.codecArguments());
            command.add(temporary.toString());

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            if (process.waitFor() != 0) {
                throw new IOException("FFmpeg failed: " + output);
            }

            AudioFileIO.read(temporary.toFile());
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            var convertedAudioFile = AudioFileIO.read(target.toFile());
            Files.delete(source);
            song.replaceAudioFile(convertedAudioFile);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Set<String> extensionsIn(Album album) {
        Set<String> formats = new TreeSet<>();
        for (Song song : songsIn(album)) {
            formats.add(extensionOf(song.getAudioFile().getFile().toPath()));
        }
        return formats;
    }

    private static AudioFormat recommendedFormat(Album album, Set<String> formats) {
        Map<String, Integer> counts = new HashMap<>();
        for (Song song : songsIn(album)) {
            counts.merge(extensionOf(song.getAudioFile().getFile().toPath()), 1, Integer::sum);
        }
        String mostCommon = formats.stream()
                .max(Comparator.comparingInt(format -> counts.getOrDefault(format, 0)))
                .orElse("");
        return Arrays.stream(AudioFormat.values())
                .filter(format -> format.extension().equals(mostCommon))
                .findFirst()
                .orElse(AudioFormat.FLAC);
    }

    private static List<Song> songsIn(Album album) {
        return album.getDiscs().stream().map(Disc::getSongs).flatMap(Collection::stream).toList();
    }

    private static String extensionOf(Path file) {
        String name = file.getFileName().toString();
        int extensionStart = name.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == name.length() - 1) {
            return "(no extension)";
        }
        return name.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
    }

    private static String formatDescription(Set<String> formats) {
        return formats.stream().map(format -> format.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
