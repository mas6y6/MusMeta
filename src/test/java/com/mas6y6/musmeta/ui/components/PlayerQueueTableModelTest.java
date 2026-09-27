package com.mas6y6.musmeta.ui.components;

import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.musicplayer.MusicPlayer;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerQueueTableModelTest {

    @TempDir
    Path tempDir;

    private final MusicPlayer player = MusicPlayer.getInstance();

    private final PlayerQueueTableModel model = new PlayerQueueTableModel();

    @BeforeEach
    void setUp() {
        player.stop();
        player.clearQueue();
        model.reload();
    }

    private Song song(String title) throws Exception {
        ID3v24Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.TITLE, title);
        tag.setField(FieldKey.ARTIST, "Artist " + title);
        tag.setField(FieldKey.ALBUM, "Album " + title);

        File file = tempDir.resolve(title + ".mp3").toFile();

        return new Song(new AudioFile(file, new GenericAudioHeader(), tag));
    }

    private void queue(String... titles) throws Exception {
        Song[] songs = new Song[titles.length];

        for (int i = 0; i < titles.length; i++) {
            songs[i] = song(titles[i]);
        }

        player.addToQueue(songs);

        // The model only ever mirrors the player, so a queue filled from the
        // outside is picked up the same way the dialog picks it up.
        model.reload();
    }

    private List<String> queuedTitles() {
        return player.getQueue()
                .stream()
                .map(Song::getTitle)
                .toList();
    }

    @Test
    void testTheColumnsAreTheQueuePositionAndTheSong() {
        assertEquals(4, model.getColumnCount());
        assertEquals("#", model.getColumnName(0));
        assertEquals("Title", model.getColumnName(1));
        assertEquals("Artist", model.getColumnName(2));
        assertEquals("Album", model.getColumnName(3));
    }

    @Test
    void testReloadShowsTheQueueThePlayerHas() throws Exception {
        queue("One", "Two");

        model.reload();

        assertEquals(2, model.getRowCount());
        assertEquals(1, model.getValueAt(0, 0));
        assertEquals(2, model.getValueAt(1, 0));
        assertEquals("One", model.getValueAt(0, 1));
        assertEquals("Artist Two", model.getValueAt(1, 2));
        assertEquals("Album Two", model.getValueAt(1, 3));
    }

    @Test
    void testAnEmptyQueueGivesAnEmptyTable() {
        assertEquals(0, model.getRowCount());
    }

    @Test
    void testRemoveTakesTheSongOffThePlayersQueue() throws Exception {
        queue("One", "Two", "Three");

        model.remove(1);

        assertEquals(List.of("One", "Three"), queuedTitles());
        assertEquals(2, model.getRowCount());
        assertEquals("Three", model.getValueAt(1, 1));
        assertEquals(2, model.getValueAt(1, 0));
    }

    @Test
    void testMoveRowReordersThePlayersQueue() throws Exception {
        queue("One", "Two", "Three");

        model.moveRow(2, 0);

        assertEquals(List.of("Three", "One", "Two"), queuedTitles());
        assertEquals("Three", model.getValueAt(0, 1));
        assertEquals(1, model.getValueAt(0, 0));
    }

    @Test
    void testTypingANewPositionMovesTheSongThere() throws Exception {
        queue("One", "Two", "Three");

        // What the queue position column does when it is edited.
        model.setValueAt("3", 0, 0);

        assertEquals(List.of("Two", "Three", "One"), queuedTitles());
        assertEquals("One", model.getValueAt(2, 1));
    }

    @Test
    void testAPositionThatIsNotOnTheListChangesNothing() throws Exception {
        queue("One", "Two", "Three");

        model.setValueAt("0", 0, 0);
        model.setValueAt("4", 0, 0);
        model.setValueAt("not a number", 0, 0);
        model.setValueAt(null, 0, 0);

        assertEquals(List.of("One", "Two", "Three"), queuedTitles());
    }

    @Test
    void testClearEmptiesThePlayersQueue() throws Exception {
        queue("One", "Two");

        model.clear();

        assertTrue(queuedTitles().isEmpty());
        assertEquals(0, model.getRowCount());
    }

    @Test
    void testOnlyThePositionColumnCanBeEdited() throws Exception {
        queue("One");

        assertTrue(model.isCellEditable(0, 0));
        assertFalse(model.isCellEditable(0, 1));
        assertFalse(model.isCellEditable(0, 2));
        assertFalse(model.isCellEditable(0, 3));
    }
}
