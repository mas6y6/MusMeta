package com.mas6y6.musmeta.ui.components;

import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.musicplayer.MusicPlayer;

import java.util.List;

/**
 * A {@link QueueTableModel} showing the {@link MusicPlayer}'s queue.
 *
 * <p>The player owns the queue, this only mirrors it. Every change made through
 * the model is handed to the player, and the model then shows whatever the
 * player ended up with, so the table and the queue can never drift apart.
 * Call {@link #reload()} after the queue was changed from anywhere else.
 */
public class PlayerQueueTableModel extends QueueTableModel<Song> {

    public PlayerQueueTableModel() {
        super(
                new String[]{"Title", "Artist", "Album"},
                Song::getTitle,
                Song::getArtist,
                Song::getAlbum
        );
    }

    /**
     * Rebuilds the rows from the queue as the player currently has it.
     */
    public void reload() {
        List<Song> items = getItems();

        items.clear();
        items.addAll(MusicPlayer.getInstance().getQueue());

        fireTableDataChanged();
    }

    @Override
    public void remove(int row) {
        MusicPlayer.getInstance().removeFromQueue(row);

        reload();
    }

    @Override
    public void clear() {
        MusicPlayer.getInstance().clearQueue();

        reload();
    }

    @Override
    public void moveRow(int from, int to) {
        MusicPlayer.getInstance().moveInQueue(from, to);

        reload();
    }
}
