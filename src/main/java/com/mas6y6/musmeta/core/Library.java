package com.mas6y6.musmeta.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mas6y6.musmeta.config.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Holds the music library (albums and their disc/song assortment) and
 * handles persisting it to the config through its {@link ConfigCodec}.
 */
public class Library {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Library.class);

    /**
     * A song that was listed in the saved library but could not be read back
     * from the path it was stored with.
     */
    public record MissingSong(String path, String albumTitle, int discIndex) {
    }

    private static final String CONFIG_NAME = "library";
    private static final String CONFIG_KEY = "data";
    private static final int FORMAT_VERSION = 1;

    public static final ConfigCodec<Library> CODEC = ConfigCodec.of(
            Library::serialize,
            Library::decodeFrom
    );

    private final TreeMap<String, Album> albums = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final ArrayList<MissingSong> missingSongs = new ArrayList<>();

    private static final Library INSTANCE = new Library();

    static {
        ensureRegistered();
    }

    public static final ConfigContainer<Library> CONFIG = ConfigManager.getInstance().getConfig(CONFIG_NAME).getContainer(CONFIG_KEY);

    private Library() {
    }

    public static Library getInstance() {
        return INSTANCE;
    }

    public List<Album> getAlbums() {
        return List.copyOf(albums.values());
    }

    public Map<String, Album> getAlbumsByTitle() {
        return Collections.unmodifiableMap(albums);
    }

    public Album getAlbum(String title) {
        if (title == null) {
            return null;
        }
        return albums.get(title);
    }

    public boolean containsAlbum(String title) {
        return title != null && albums.containsKey(title);
    }

    public void registerAlbum(Album album) {
        Objects.requireNonNull(album, "Album cannot be null");
        albums.put(album.getTitle(), album);
    }

    public void renameAlbum(Album album, String newTitle) {
        Objects.requireNonNull(album, "Album cannot be null");
        Objects.requireNonNull(newTitle, "New album title cannot be null");
        albums.entrySet().removeIf(entry -> entry.getValue() == album);
        album.setTitle(newTitle);
        albums.put(newTitle, album);
        save();
    }

    public void removeAlbum(String title) {
        if (title != null) {
            albums.remove(title);
            save();
        }
    }

    public List<Song> getSongs() {
        ArrayList<Song> allSongs = new ArrayList<>();
        for (Album album : albums.values()) {
            allSongs.addAll(album.getSongs());
        }
        return List.copyOf(allSongs);
    }

    public List<MissingSong> getMissingSongs() {
        ArrayList<MissingSong> result = new ArrayList<>(missingSongs);
        for (Album album : albums.values()) {
            for (Disc disc : album.getDiscs()) {
                for (String path : disc.getMissingSongPaths()) {
                    result.add(new MissingSong(path, album.getTitle(), disc.getDiscIndex()));
                }
            }
        }
        return List.copyOf(result);
    }

    /**
     * Adds a song to the library, placing it into the album declared by its tags.
     */
    public void addSong(Song song) {
        Objects.requireNonNull(song, "Song cannot be null");
        String albumTitle = song.getAlbum();
        Album album = albums.computeIfAbsent(albumTitle, Album::new);
        album.addSong(song);
    }

    public void clear() {
        albums.clear();
        missingSongs.clear();
    }

    public void removeSong(Song song) {
        if (song == null) {
            return;
        }
        for (Album album : new ArrayList<>(albums.values())) {
            album.removeSong(song);
            if (album.getSongs().isEmpty()) {
                albums.remove(album.getTitle());
            }
        }
    }

    public void reorganizeSong(Song song) {
        removeSong(song);
        addSong(song);
    }

    void replaceWith(Library other) {
        clear();
        if (other != null) {
            albums.putAll(other.albums);
            missingSongs.addAll(other.missingSongs);
        }
    }

    private static void serialize(Library library, ConfigBuilder builder) {
        builder.setInt("version", FORMAT_VERSION);

        JsonArray albumsArray = new JsonArray();
        for (Album album : library.albums.values()) {
            ConfigBuilder albumBuilder = new ConfigBuilder();
            Album.CODEC.encode(album, albumBuilder);
            albumsArray.add(albumBuilder.toJsonObject());
        }
        builder.set("albums", albumsArray);
    }

    private static Library decodeFrom(ConfigBuilder builder) {
        Library library = new Library();

        JsonElement albumsElement = builder.get("albums");
        if (albumsElement == null || !albumsElement.isJsonArray()) {
            return library;
        }

        for (JsonElement albumElement : albumsElement.getAsJsonArray()) {
            if (!albumElement.isJsonObject()) {
                continue;
            }
            ConfigBuilder albumBuilder = ConfigBuilder.from(albumElement.getAsJsonObject());
            Album album = Album.CODEC.decode(albumBuilder);
            if (album != null) {
                library.albums.put(album.getTitle(), album);
            }
        }

        return library;
    }

    /**
     * Persists the library to the config through the registered codec.
     */
    public void save() {
        ensureRegistered();
        ConfigManager.getInstance().getConfig(CONFIG_NAME).setValue(CONFIG_KEY, this);
    }

    /**
     * Restores the library from the config, replacing the current contents.
     */
    public static Library load() {
        ensureRegistered();

        SubConfig config = ConfigManager.getInstance().getConfig(CONFIG_NAME);
        Library loaded = config != null ? config.getValue(CONFIG_KEY) : null;
        if (loaded != null && loaded != INSTANCE) {
            INSTANCE.replaceWith(loaded);
        }
        return INSTANCE;
    }

    private static synchronized void ensureRegistered() {
        ConfigManager.registerCodec(Song.class, Song.CODEC);
        ConfigManager.registerCodec(Disc.class, Disc.CODEC);
        ConfigManager.registerCodec(Album.class, Album.CODEC);
        ConfigManager.registerCodec(Library.class, CODEC);

        ConfigManager configManager = ConfigManager.getInstance();
        SubConfig config = configManager.getOrCreateConfig(CONFIG_NAME);
        if (config.getContainer(CONFIG_KEY) == null) {
            config.register(CONFIG_KEY, INSTANCE, Library.class);
        }
    }
    
    
}