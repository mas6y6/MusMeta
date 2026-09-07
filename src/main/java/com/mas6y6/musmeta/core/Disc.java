package com.mas6y6.musmeta.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mas6y6.musmeta.config.ConfigBuilder;
import com.mas6y6.musmeta.config.ConfigCodec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Disc {
    public static final ConfigCodec<Disc> CODEC = ConfigCodec.of(
            Disc::serialize,
            Disc::decodeFrom
    );

    private final int discIndex;
    private int discTotal;
    private final ArrayList<Song> songs = new ArrayList<>();
    private final ArrayList<String> missingSongPaths = new ArrayList<>();

    public Disc(int discIndex, int discTotal) {
        this.discIndex = discIndex;
        this.discTotal = discTotal;
    }

    public int getDiscIndex() {
        return discIndex;
    }

    public int getDiscTotal() {
        return discTotal;
    }

    public List<Song> getSongs() {
        return List.copyOf(songs);
    }

    public List<String> getMissingSongPaths() {
        return List.copyOf(missingSongPaths);
    }

    public void addMissingSongPath(String path) {
        if (path != null && !path.isBlank()) {
            missingSongPaths.add(path);
        }
    }

    void setDiscTotal(int discTotal) {
        this.discTotal = Math.max(this.discTotal, discTotal);
    }

    void addSong(Song song) {
        songs.add(song);
        resortSongs();
    }

    public void removeSong(Song song) {
        songs.remove(song);
    }

    public void resortSongs() {
        songs.sort(Comparator
                .comparingInt((Song s) -> s.getTrackNumber() == 0 ? Integer.MAX_VALUE : s.getTrackNumber())
                .thenComparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER));
    }

    private static void serialize(Disc disc, ConfigBuilder builder) {
        builder.setInt("index", disc.getDiscIndex());
        builder.setInt("total", disc.getDiscTotal());

        JsonArray songsArray = new JsonArray();
        for (Song song : disc.getSongs()) {
            ConfigBuilder songBuilder = new ConfigBuilder();
            Song.CODEC.encode(song, songBuilder);
            songsArray.add(songBuilder.toJsonObject());
        }
        for (String missingPath : disc.getMissingSongPaths()) {
            ConfigBuilder songBuilder = new ConfigBuilder();
            songBuilder.setString("path", missingPath);
            songsArray.add(songBuilder.toJsonObject());
        }
        builder.set("songs", songsArray);
    }

    private static Disc decodeFrom(ConfigBuilder builder) {
        if (builder == null) {
            return null;
        }
        int index = builder.getInt("index", 1);
        int total = builder.getInt("total", index);
        Disc disc = new Disc(index, total);

        JsonElement songsElement = builder.get("songs");
        if (songsElement != null && songsElement.isJsonArray()) {
            for (JsonElement songElement : songsElement.getAsJsonArray()) {
                ConfigBuilder songBuilder;
                if (songElement.isJsonObject()) {
                    songBuilder = ConfigBuilder.from(songElement.getAsJsonObject());
                } else if (songElement.isJsonPrimitive()) {
                    songBuilder = new ConfigBuilder();
                    songBuilder.set("value", songElement.getAsString());
                } else {
                    continue;
                }
                String path = songBuilder.getString("path", songBuilder.getString("value"));
                Song song = Song.CODEC.decode(songBuilder);
                if (song != null) {
                    disc.addSong(song);
                } else if (path != null && !path.isBlank()) {
                    disc.addMissingSongPath(path);
                }
            }
        }
        return disc;
    }
}