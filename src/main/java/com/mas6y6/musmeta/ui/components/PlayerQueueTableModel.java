package com.mas6y6.musmeta.ui.components;

import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.musicplayer.MusicPlayer;

import java.util.List;

public class PlayerQueueTableModel extends QueueTableModel<Song> {

    public PlayerQueueTableModel() {
        super(
                new String[]{"Title", "Artist", "Album"},
                Song::getTitle,
                Song::getArtist,
                Song::getAlbum
        );
    }

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
