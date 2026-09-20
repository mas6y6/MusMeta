package com.mas6y6.musmeta.musicplayer;

import com.mas6y6.musmeta.audio.AudioManager;
import com.mas6y6.musmeta.audio.FFmpegAudioStream;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class MusicPlayer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MusicPlayer.class);

    private static final Duration RESTART_THRESHOLD =
            Duration.ofSeconds(3);

    private static final int NO_SKIP = Integer.MIN_VALUE;

    private static MusicPlayer instance;

    public interface Listener {

        default void stateUpdated(
                boolean isPlaying,
                boolean isPaused,
                boolean isStopped
        ) {}

        default void songChanged(Song song) {}

        default void musicPlayerStopped() {}
    }

    private final CopyOnWriteArrayList<Song> queue =
            new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<Listener> listeners =
            new CopyOnWriteArrayList<>();

    private final AtomicInteger pendingSkipIndex =
            new AtomicInteger(NO_SKIP);

    private volatile Thread thread;

    private volatile boolean isPlaying = false;
    private volatile boolean stopped = true;
    private volatile boolean everStarted = false;
    private volatile int currentSongIndex = -1;

    private MusicPlayer() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public static MusicPlayer getInstance() {
        if (instance == null) {
            instance = new MusicPlayer();
        }

        return instance;
    }

    public List<Song> getQueue() {
        return List.copyOf(queue);
    }

    public void addToQueue(Song... songs) {
        if (songs == null || songs.length == 0) {
            return;
        }

        queue.addAll(List.of(songs));
    }

    public Song getCurrentSong() {
        int index = currentSongIndex;

        if (index < 0 || index >= queue.size()) {
            return null;
        }

        return queue.get(index);
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    public boolean start() {
        if (isLoopRunning()) {
            return false;
        }

        if (queue.isEmpty()) {
            return false;
        }

        stopped = false;
        everStarted = true;
        isPlaying = false;

        pendingSkipIndex.set(NO_SKIP);

        if (currentSongIndex < 0 || currentSongIndex >= queue.size()) {
            currentSongIndex = 0;
        }

        thread = Thread.ofVirtual()
                .name("MusicPlayer")
                .start(this::playbackLoop);

        return true;
    }

    private void playbackLoop() {
        try {
            while (!stopped) {
                if (queue.isEmpty()) {
                    stopInternal();
                    break;
                }

                int pending = pendingSkipIndex.getAndSet(NO_SKIP);

                if (pending != NO_SKIP) {
                    currentSongIndex = normalizeIndex(pending);
                }

                if (currentSongIndex < 0
                        || currentSongIndex >= queue.size()) {
                    currentSongIndex = 0;
                }

                Song song = queue.get(currentSongIndex);

                isPlaying = true;
                notifyStateUpdated();

                try {
                    play(song);
                } catch (IOException e) {
                    isPlaying = false;

                    LOGGER.error(
                            "Failed to play song: {}",
                            song.getSourceAudioFile(),
                            e
                    );

                    notifyStateUpdated();
                }

                if (stopped) {
                    break;
                }

                pending = pendingSkipIndex.getAndSet(NO_SKIP);

                if (pending != NO_SKIP) {
                    currentSongIndex = normalizeIndex(pending);
                    continue;
                }

                currentSongIndex++;

                if (currentSongIndex >= queue.size()) {
                    stopInternal();
                    break;
                }

                isPlaying = false;
                notifyStateUpdated();
            }

        } catch (Exception e) {
            LOGGER.error("Unexpected error in music player", e);

            isPlaying = false;
            stopped = true;

            notifyStateUpdated();

        } finally {
            isPlaying = false;

            stopped = true;

            notifyStateUpdated();

            notifyMusicPlayerStopped();

            thread = null;
        }
    }

    private void play(Song song) throws IOException {
        notifySongChanged(song);

        AudioManager.getInstance().play(
                new FFmpegAudioStream(
                        Path.of(
                                Settings.FFMPEG_INSTALLATION_PATH.get()
                        ),
                        song.getSourceAudioFile().toPath()
                )
        );
    }

    public void stop() {
        if (stopped && !isLoopRunning()) {
            return;
        }

        stopped = true;
        isPlaying = false;

        AudioManager.getInstance().stop();

        notifyStateUpdated();
    }

    private void stopInternal() {
        stopped = true;
        isPlaying = false;

        notifyStateUpdated();
    }

    public void pause() {
        if (!isPlaying) {
            return;
        }

        AudioManager.getInstance().pause();

        isPlaying = false;
        notifyStateUpdated();
    }

    public void resume() {
        if (isPlaying || !isLoopRunning()) {
            return;
        }

        if (!AudioManager.getInstance().isPaused()) {
            return;
        }

        AudioManager.getInstance().resume();

        isPlaying = true;
        notifyStateUpdated();
    }

    public void next() {
        if (queue.isEmpty() || !everStarted) {
            return;
        }

        int target = currentSongIndex + 1;

        if (target >= queue.size()) {
            target = 0;
        }

        skipTo(target);
    }

    public void previous() {
        if (queue.isEmpty() || !everStarted) {
            return;
        }

        if (isLoopRunning()
                && AudioManager.getInstance()
                .getPosition()
                .compareTo(RESTART_THRESHOLD) > 0) {

            AudioManager.getInstance().seek(Duration.ZERO);
            return;
        }

        int target = currentSongIndex - 1;

        if (target < 0) {
            target = queue.size() - 1;
        }

        skipTo(target);
    }

    private void skipTo(int targetIndex) {
        if (queue.isEmpty()) {
            return;
        }

        targetIndex = normalizeIndex(targetIndex);

        currentSongIndex = targetIndex;

        if (isLoopRunning()) {
            pendingSkipIndex.set(targetIndex);

            AudioManager.getInstance().stop();

        } else {
            start();
        }
    }

    private int normalizeIndex(int index) {
        int size = queue.size();

        if (size == 0) {
            return -1;
        }

        return Math.floorMod(index, size);
    }

    private boolean isLoopRunning() {
        Thread currentThread = thread;

        return currentThread != null && currentThread.isAlive();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isPaused() {
        return AudioManager.getInstance().isPaused();
    }

    public void addListener(Listener listener) {
        if (listener == null) {
            return;
        }

        listeners.addIfAbsent(listener);
    }

    public void removeListener(Listener listener) {
        if (listener == null) {
            return;
        }

        listeners.remove(listener);
    }

    private void notifyStateUpdated() {
        boolean playing = isPlaying();
        boolean paused = isPaused();
        boolean isStopped = isStopped();

        for (Listener listener : listeners) {
            try {
                listener.stateUpdated(
                        playing,
                        paused,
                        isStopped
                );
            } catch (Exception e) {
                LOGGER.warn(
                        "MusicPlayer listener threw an exception",
                        e
                );
            }
        }
    }

    private void notifySongChanged(Song song) {
        for (Listener listener : listeners) {
            try {
                listener.songChanged(song);
            } catch (Exception e) {
                LOGGER.warn(
                        "MusicPlayer listener threw an exception",
                        e
                );
            }
        }
    }

    private void notifyMusicPlayerStopped() {
        for (Listener listener : listeners) {
            try {
                listener.musicPlayerStopped();
            } catch (Exception e) {
                LOGGER.warn(
                        "MusicPlayer listener threw an exception",
                        e
                );
            }
        }
    }
}