package com.mas6y6.musmeta.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mas6y6.musmeta.config.*;
import org.jaudiotagger.audio.AudioFileIO;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Holds the music library (songs and their album/disc assortment) and
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

    private static final ConfigCodec<Library> CODEC = ConfigCodec.of(
            Library::serialize,
            Library::decodeFrom
    );

    private final ArrayList<Album> albums = new ArrayList<>();
    private final Map<String, Album> albumsByTitle = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
        return albums.stream()
                .sorted(Comparator
                        .comparingLong(Album::getCreatedAt).reversed()
                        .thenComparing(Album::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Map<String, Album> getAlbumsByTitle() {
        return Map.copyOf(albumsByTitle);
    }

    public boolean containsAlbum(String title) {
        return albumsByTitle.containsKey(title);
    }

    public List<Song> getSongs() {
        ArrayList<Song> songs = new ArrayList<>();
        for (Album album : albums) {
            songs.addAll(album.getSongs());
        }
        return List.copyOf(songs);
    }

    public List<MissingSong> getMissingSongs() {
        return List.copyOf(missingSongs);
    }

    /**
     * Adds a song and places it into its album and disc (iTunes-style).
     */
    public void addSong(Song song) {
        Objects.requireNonNull(song, "Song cannot be null");

        Album album = albumsByTitle.computeIfAbsent(song.getAlbum(), title -> {
            Album created = new Album(title);
            albums.add(created);
            return created;
        });

        album.addSong(song);
    }

    public void clear() {
        albums.clear();
        albumsByTitle.clear();
        missingSongs.clear();
    }

    void replaceWith(Library other) {
        clear();
        if (other != null) {
            for (Album album : other.getAlbums()) {
                registerAlbum(album);
            }
            missingSongs.addAll(other.missingSongs);
        }
    }

    public void registerAlbum(Album album) {
        if (albumsByTitle.containsKey(album.getTitle())) {
            throw new IllegalArgumentException("Album with title '" + album.getTitle() + "' already exists");
        }

        albumsByTitle.put(album.getTitle(), album);
        albums.add(album);
    }

    private static void serialize(Library library, ConfigBuilder builder) {
        JsonArray albumsArray = new JsonArray();

        for (Album album : library.getAlbums()) {
            JsonObject albumObject = new JsonObject();
            albumObject.addProperty("title", album.getTitle());
            albumObject.addProperty("createdAt", album.getCreatedAt());

            Path artworkPath = album.getArtworkPath();
            if (artworkPath != null) {
                albumObject.addProperty("artworkPath", artworkPath.toString());
            }

            JsonArray discsArray = new JsonArray();
            for (Disc disc : album.getDiscs()) {
                JsonObject discObject = new JsonObject();
                discObject.addProperty("index", disc.getDiscIndex());
                discObject.addProperty("total", disc.getDiscTotal());

                JsonArray songsArray = new JsonArray();
                for (Song song : disc.getSongs()) {
                    songsArray.add(song.getAudioFile().getFile().getAbsolutePath());
                }
                discObject.add("songs", songsArray);
                discsArray.add(discObject);
            }
            albumObject.add("discs", discsArray);
            albumsArray.add(albumObject);
        }

        builder.setInt("version", FORMAT_VERSION);
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
            JsonObject albumObject = albumElement.getAsJsonObject();

            String title = albumObject.has("title")
                    ? albumObject.get("title").getAsString()
                    : "Unknown Album";

            Path artworkPath = null;
            if (albumObject.has("artworkPath")) {
                String raw = albumObject.get("artworkPath").getAsString();
                if (raw != null && !raw.isBlank()) {
                    artworkPath = Path.of(raw);
                }
            }

            long createdAt = albumObject.has("createdAt")
                    ? albumObject.get("createdAt").getAsLong()
                    : System.currentTimeMillis();

            Album album = new Album(title, artworkPath, createdAt);

            JsonElement discsElement = albumObject.get("discs");
            if (discsElement != null && discsElement.isJsonArray()) {
                for (JsonElement discElement : discsElement.getAsJsonArray()) {
                    if (!discElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject discObject = discElement.getAsJsonObject();

                    int index = discObject.has("index") ? discObject.get("index").getAsInt() : 1;
                    int total = discObject.has("total") ? discObject.get("total").getAsInt() : index;
                    Disc disc = new Disc(index, total);

                    JsonElement songsElement = discObject.get("songs");
                    if (songsElement != null && songsElement.isJsonArray()) {
                        for (JsonElement songElement : songsElement.getAsJsonArray()) {
                            String path = songElement.getAsString();
                            Song song = readSong(path);
                            if (song == null) {
                                library.missingSongs.add(new MissingSong(path, title, index));
                                continue;
                            }
                            disc.addSong(song);
                        }
                    }
                    album.addDisc(disc);
                }
            }

            library.registerAlbum(album);
        }

        return library;
    }

    private static Song readSong(String path) {
        try {
            return new Song(AudioFileIO.read(Path.of(path).toFile()));
        } catch (Exception e) {
            LOGGER.warn("Skipping unreadable song in saved library: {}", path);
            return null;
        }
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
        ConfigManager.registerCodec(Library.class, CODEC);

        ConfigManager configManager = ConfigManager.getInstance();
        SubConfig config = configManager.getOrCreateConfig(CONFIG_NAME);
        if (config.getContainer(CONFIG_KEY) == null) {
            config.register(CONFIG_KEY, INSTANCE, Library.class);
        }
    }
}