package com.mas6y6.musmeta.musicplayer;

import com.mas6y6.musmeta.core.Song;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MusicPlayerQueueTest {

    @TempDir
    Path tempDir;

    private final MusicPlayer player = MusicPlayer.getInstance();

    @BeforeEach
    void setUp() {
        player.stop();
        player.clearQueue();
    }

    private Song song(String title) throws Exception {
        ID3v24Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.TITLE, title);
        tag.setField(FieldKey.ARTIST, "Artist " + title);
        tag.setField(FieldKey.ALBUM, "Album " + title);

        File file = tempDir.resolve(title + ".mp3").toFile();

        return new Song(new AudioFile(file, new GenericAudioHeader(), tag));
    }

    private List<String> queuedTitles() {
        return player.getQueue()
                .stream()
                .map(Song::getTitle)
                .toList();
    }

    private void queue(String... titles) throws Exception {
        for (String title : titles) {
            player.addToQueue(song(title));
        }
    }

    @Test
    void testAddToQueueKeepsTheOrderSongsWereAddedIn() throws Exception {
        queue("One", "Two", "Three");

        assertEquals(List.of("One", "Two", "Three"), queuedTitles());
        assertEquals(3, player.getQueue().size());
    }

    @Test
    void testAddToQueueIgnoresAnEmptySelection() {
        assertDoesNotThrow(() -> player.addToQueue());
        assertDoesNotThrow(() -> player.addToQueue((Song[]) null));

        assertTrue(player.getQueue().isEmpty());
    }

    @Test
    void testGetQueueIsAnImmutableSnapshot() throws Exception {
        queue("One", "Two");

        List<Song> snapshot = player.getQueue();

        assertThrows(UnsupportedOperationException.class, snapshot::clear);

        player.addToQueue(song("Three"));

        assertEquals(2, snapshot.size());
        assertEquals(3, player.getQueue().size());
    }

    @Test
    void testRemoveFromQueueDropsOnlyThatSong() throws Exception {
        queue("One", "Two", "Three");

        player.removeFromQueue(1);

        assertEquals(List.of("One", "Three"), queuedTitles());
    }

    @Test
    void testRemoveFromQueueIgnoresPositionsThatAreNotThere() throws Exception {
        queue("One", "Two");

        player.removeFromQueue(-1);
        player.removeFromQueue(2);
        player.removeFromQueue(99);

        assertEquals(List.of("One", "Two"), queuedTitles());
    }

    @Test
    void testRemoveFromQueueOnAnEmptyQueueDoesNothing() {
        assertDoesNotThrow(() -> player.removeFromQueue(0));

        assertTrue(player.getQueue().isEmpty());
    }

    @Test
    void testMoveInQueueReordersTheSongs() throws Exception {
        queue("One", "Two", "Three", "Four");

        player.moveInQueue(3, 0);

        assertEquals(
                List.of("Four", "One", "Two", "Three"),
                queuedTitles()
        );
    }

    @Test
    void testMoveInQueueClampsTheTargetPosition() throws Exception {
        queue("One", "Two", "Three");

        player.moveInQueue(0, 99);

        assertEquals(List.of("Two", "Three", "One"), queuedTitles());

        player.moveInQueue(2, -5);

        assertEquals(List.of("One", "Two", "Three"), queuedTitles());
    }

    @Test
    void testMoveInQueueIgnoresPositionsThatAreNotThere() throws Exception {
        queue("One", "Two");

        player.moveInQueue(-1, 0);
        player.moveInQueue(5, 0);
        player.moveInQueue(0, 0);

        assertEquals(List.of("One", "Two"), queuedTitles());
    }

    @Test
    void testClearQueueEmptiesTheQueue() throws Exception {
        queue("One", "Two", "Three");

        player.clearQueue();

        assertTrue(player.getQueue().isEmpty());
        assertNull(player.getCurrentSong());
        assertEquals(-1, player.getCurrentSongIndex());
    }

    @Test
    void testEveryQueueChangeIsAnnounced() throws Exception {
        AtomicInteger changes = new AtomicInteger();

        MusicPlayer.Listener listener = new MusicPlayer.Listener() {
            @Override
            public void queueChanged() {
                changes.incrementAndGet();
            }
        };

        player.addListener(listener);

        try {
            int before = changes.get();

            player.addToQueue(song("One"));
            player.addToQueue(song("Two"));

            player.moveInQueue(0, 1);

            player.removeFromQueue(1);

            player.clearQueue();

            assertEquals(5, changes.get() - before);
        } finally {
            player.removeListener(listener);
        }
    }

    @Test
    void testAFailingListenerDoesNotStopTheOthers() throws Exception {
        List<String> seen = new ArrayList<>();

        MusicPlayer.Listener broken = new MusicPlayer.Listener() {
            @Override
            public void queueChanged() {
                throw new IllegalStateException("boom");
            }
        };

        MusicPlayer.Listener working = new MusicPlayer.Listener() {
            @Override
            public void queueChanged() {
                seen.add(player.getQueue().size() + "");
            }
        };

        player.addListener(broken);
        player.addListener(working);

        try {
            queue("One", "Two");

            assertEquals(List.of("1", "2"), seen);
        } finally {
            player.removeListener(broken);
            player.removeListener(working);
        }
    }
}
