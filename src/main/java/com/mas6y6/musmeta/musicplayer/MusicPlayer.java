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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * How far into a track the user must be before {@link #previous()}
     * restarts the current track instead of going to the previous track.
     */
    private static final Duration RESTART_THRESHOLD =
            Duration.ofSeconds(3);

    /**
     * Indicates that there is no pending skip request.
     */
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

    /*
     * CopyOnWriteArrayList works well here because the queue is generally
     * read much more often than it is modified.
     */
    private final CopyOnWriteArrayList<Song> queue =
            new CopyOnWriteArrayList<>();

    /*
     * Listeners are typically read on every state update but only added or
     * removed occasionally.
     */
    private final CopyOnWriteArrayList<Listener> listeners =
            new CopyOnWriteArrayList<>();

    /**
     * The track that should be played after the currently-blocking
     * AudioManager.play() call returns.
     */
    private final AtomicInteger pendingSkipIndex =
            new AtomicInteger(NO_SKIP);

    private volatile Thread thread;

    private volatile boolean isPlaying = false;
    private volatile boolean stopped = true;
    private volatile boolean everStarted = false;
    private volatile int currentSongIndex = -1;

    private MusicPlayer() {
    }

    public static MusicPlayer getInstance() {
        if (instance == null) {
            instance = new MusicPlayer();
        }

        return instance;
    }

    // -------------------------------------------------------------------------
    // Queue
    // -------------------------------------------------------------------------

    /**
     * Returns a snapshot of the current queue.
     *
     * @return an immutable snapshot of the queue
     */
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

    // -------------------------------------------------------------------------
    // Playback controls
    // -------------------------------------------------------------------------

    /**
     * Starts playback.
     *
     * @return true if a playback thread was started
     */
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

        /*
         * If playback has never started, begin at the first song.
         *
         * If playback was previously stopped, also start from the current
         * index if it is still valid.
         */
        if (currentSongIndex < 0 || currentSongIndex >= queue.size()) {
            currentSongIndex = 0;
        }

        thread = Thread.ofVirtual()
                .name("MusicPlayer")
                .start(this::playbackLoop);

        return true;
    }

    /**
     * Main blocking playback loop.
     *
     * AudioManager.play() is intentionally blocking. It returns when the
     * track finishes naturally or when AudioManager.stop() is called.
     */
    private void playbackLoop() {
        try {
            while (!stopped) {

                // Queue was emptied while we were playing.
                if (queue.isEmpty()) {
                    stopInternal();
                    break;
                }

                /*
                 * A skip may have been requested while the previous
                 * AudioManager.play() call was blocking.
                 */
                int pending = pendingSkipIndex.getAndSet(NO_SKIP);

                if (pending != NO_SKIP) {
                    currentSongIndex = normalizeIndex(pending);
                }

                /*
                 * Make sure the current index is still valid.
                 */
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

                    /*
                     * Don't completely kill the player because one broken
                     * file was encountered. Move to the next track.
                     */
                }

                /*
                 * play() has returned.
                 *
                 * If stop() was called, we're done.
                 */
                if (stopped) {
                    break;
                }

                /*
                 * If next()/previous() requested a specific track while
                 * play() was blocking, use that track instead of advancing
                 * normally.
                 */
                pending = pendingSkipIndex.getAndSet(NO_SKIP);

                if (pending != NO_SKIP) {
                    currentSongIndex = normalizeIndex(pending);
                    continue;
                }

                /*
                 * The song finished naturally, so advance to the next one.
                 */
                currentSongIndex++;

                /*
                 * End of queue.
                 *
                 * This version stops at the end rather than automatically
                 * looping back to the first song.
                 */
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

            /*
             * Only clear the stopped state if the player naturally reached
             * the end of the queue.
             */
            stopped = true;

            notifyStateUpdated();

            notifyMusicPlayerStopped();

            thread = null;
        }
    }

    /**
     * Actually plays a song.
     *
     * This method is intentionally blocking.
     */
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

    /**
     * Stops playback completely.
     */
    public void stop() {
        if (stopped && !isLoopRunning()) {
            return;
        }

        stopped = true;
        isPlaying = false;

        /*
         * This MUST cause AudioManager.play() to return.
         */
        AudioManager.getInstance().stop();

        notifyStateUpdated();
    }

    /**
     * Internal stop used by the playback thread.
     */
    private void stopInternal() {
        stopped = true;
        isPlaying = false;

        notifyStateUpdated();
    }

    /**
     * Pauses the current track.
     */
    public void pause() {
        if (!isPlaying) {
            return;
        }

        AudioManager.getInstance().pause();

        isPlaying = false;
        notifyStateUpdated();
    }

    /**
     * Resumes playback if the player loop is still alive.
     */
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

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /**
     * Skips to the next track.
     *
     * Wraps around to the beginning of the queue.
     */
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

    /**
     * Goes to the previous track.
     *
     * If the current track has played for more than
     * {@link #RESTART_THRESHOLD}, the current track is restarted instead.
     */
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

    /**
     * Moves to a specific track.
     *
     * If playback is currently active, AudioManager.stop() unblocks the
     * blocking play() call. The playback loop then sees pendingSkipIndex
     * and immediately starts the requested track.
     */
    private void skipTo(int targetIndex) {
        if (queue.isEmpty()) {
            return;
        }

        targetIndex = normalizeIndex(targetIndex);

        currentSongIndex = targetIndex;

        if (isLoopRunning()) {

            /*
             * Tell the playback loop where to go next.
             */
            pendingSkipIndex.set(targetIndex);

            /*
             * This should synchronously cause AudioManager.play() to return.
             */
            AudioManager.getInstance().stop();

        } else {
            /*
             * No playback loop exists, so start one.
             */
            start();
        }
    }

    /**
     * Normalizes an index so that it wraps around the queue.
     */
    private int normalizeIndex(int index) {
        int size = queue.size();

        if (size == 0) {
            return -1;
        }

        return Math.floorMod(index, size);
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Listeners
    // -------------------------------------------------------------------------

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