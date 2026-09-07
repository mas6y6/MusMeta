package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialog;
import com.mas6y6.musmeta.utils.AlbumFormatNormalizer;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotReadVideoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ProcessMusicDialog extends ProcessingDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessMusicDialog.class);

    private final File[] files;
    private final List<Song> songsWithoutTags;
    private final List<Song> processedSongs;

    public ProcessMusicDialog(Window owner, File[] files) {
        super(owner, "Processing audio files");
        this.files = files != null ? files : new File[0];
        this.songsWithoutTags = new ArrayList<>();
        this.processedSongs = new ArrayList<>();
    }

    public List<Song> getSongsWithoutTags() {
        return Collections.unmodifiableList(songsWithoutTags);
    }

    public List<Song> getProcessedSongs() {
        return Collections.unmodifiableList(processedSongs);
    }

    private List<File> collectAudioFiles(File[] inputs) {
        List<File> result = new ArrayList<>();
        if (inputs == null) {
            return result;
        }
        for (File input : inputs) {
            if (input == null || !input.exists()) {
                continue;
            }
            if (input.isDirectory()) {
                try (var stream = Files.walk(input.toPath())) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> {
                                String ext = com.google.common.io.Files.getFileExtension(p.toString()).toLowerCase(Locale.ROOT);
                                return Constants.MUSIC_EXTENSIONS.contains(ext);
                            })
                            .forEach(p -> result.add(p.toFile()));
                } catch (Exception e) {
                    LOGGER.warn("Failed to walk directory: {}", input, e);
                }
            } else if (input.isFile()) {
                String ext = com.google.common.io.Files.getFileExtension(input.toString()).toLowerCase(Locale.ROOT);
                if (Constants.MUSIC_EXTENSIONS.contains(ext)) {
                    result.add(input);
                }
            }
        }
        return result;
    }

    @Override
    protected boolean process() throws Exception {
        updateProgress(
                "Processing audio files",
                0,
                "Loading"
        );

        List<File> audioFiles = collectAudioFiles(files);
        if (audioFiles.isEmpty()) {
            return true;
        }

        Path musicDirSetting = Settings.MUSIC_DIRECTORY_PATH.get();
        Path musicDir = musicDirSetting != null ? musicDirSetting.toAbsolutePath().normalize() : null;
        Path musMetaDir = musicDir != null ? musicDir.resolve("MusMeta") : null;

        for (int i = 0; i < audioFiles.size(); i++) {
            File file = audioFiles.get(i);
            int percent = (int) Math.round((i * 100.0) / audioFiles.size());
            updateProgress(
                    "Processing " + (i + 1) + " of " + audioFiles.size(),
                    percent,
                    file.getName()
            );

            try {
                AudioFile audioFile = AudioFileIO.read(file);
                Song song = new Song(audioFile);

                if (song.getTag() == null) {
                    songsWithoutTags.add(song);
                }
                processedSongs.add(song);
            } catch (CannotReadVideoException e) {
                LOGGER.warn("Skipping video file: {}", file);
            } catch (CannotReadException e) {
                LOGGER.warn("Skipping unreadable audio file: {}", file);
            } catch (Exception e) {
                LOGGER.error("Error reading audio file: {}", file, e);
            }
        }

        if (!processedSongs.isEmpty()) {
            for (Song song : processedSongs) {
                Library.getInstance().addSong(song);
            }

            int converted = 0;
            if (musicDir != null) {
                converted = AlbumFormatNormalizer.convertIncompatibleToFolder(
                        processedSongs,
                        AlbumFormatNormalizer.fromSetting(Settings.AUDIO_TARGET_FORMAT.get()),
                        musicDir,
                        musMetaDir,
                        (completed, total, details) -> {
                            int percent = total > 0
                                    ? (int) Math.round(completed * 100.0 / total)
                                    : 100;
                            updateProgress(
                                    "Converting " + completed + " of " + total,
                                    percent,
                                    details != null ? "Converting " + details : details
                            );
                        }
                );
            }

            Library.getInstance().save();
        }

        return true;
    }
}
