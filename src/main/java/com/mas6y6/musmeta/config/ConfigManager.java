package com.mas6y6.musmeta.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ConfigManager {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ConfigManager.class);

    public static Gson GSON = createGson();
    private static ConfigManager instance;
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".musmeta", "config.json");

    private static final Map<Class<?>, ConfigCodec<?>> codecs = new LinkedHashMap<>();

    private Path customConfigPath;
    private final Map<String, SubConfig> configs = new LinkedHashMap<>();
    private final Map<String, JsonObject> unmappedConfigs = new LinkedHashMap<>();
    private boolean loading = false;

    private ConfigManager() {

    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public static Path getDefaultConfigPath() {
        return CONFIG_PATH;
    }

    public Path getConfigPath() {
        return customConfigPath != null ? customConfigPath : CONFIG_PATH;
    }

    public void setConfigPath(Path configPath) {
        this.customConfigPath = configPath;
    }

    public synchronized SubConfig registerConfig(String name) {
        Objects.requireNonNull(name, "Config name cannot be null");
        SubConfig config = configs.get(name);
        if (config == null) {
            config = new SubConfig(name);
            if (unmappedConfigs.containsKey(name)) {
                JsonObject jsonObject = unmappedConfigs.remove(name);
                boolean wasLoading = loading;
                loading = true;
                try {
                    config.fromJson(jsonObject, GSON);
                } finally {
                    loading = wasLoading;
                }
            }
            configs.put(name, config);
        }
        return config;
    }

    public synchronized SubConfig getConfig(String name) {
        return configs.get(name);
    }

    public synchronized SubConfig getOrCreateConfig(String name) {
        return registerConfig(name);
    }

    public synchronized boolean hasConfig(String name) {
        return configs.containsKey(name) || unmappedConfigs.containsKey(name);
    }

    public synchronized Map<String, SubConfig> getConfigs() {
        return Collections.unmodifiableMap(configs);
    }

    public synchronized boolean isLoading() {
        return loading;
    }

    public synchronized void setLoading(boolean loading) {
        this.loading = loading;
    }

    public synchronized void saveQuietly() {
        if (loading) {
            return;
        }
        try {
            save();
        } catch (Exception e) {
            LOGGER.error("Failed to auto-save config: {}", e.getMessage());
        }
    }

    public synchronized void save() throws IOException {
        save(getConfigPath());
    }

    public synchronized void save(Path path) throws IOException {
        if (path == null) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        JsonObject rootObject = new JsonObject();
        // Preserve any unmapped configs loaded previously
        for (Map.Entry<String, JsonObject> entry : unmappedConfigs.entrySet()) {
            rootObject.add(entry.getKey(), entry.getValue());
        }
        // Add all active configs
        for (Map.Entry<String, SubConfig> entry : configs.entrySet()) {
            rootObject.add(entry.getKey(), entry.getValue().toJson(GSON));
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(rootObject, writer);
        }
    }

    public synchronized void load() throws IOException {
        load(getConfigPath());
    }

    public synchronized void load(Path path) throws IOException {
        Objects.requireNonNull(path, "Path cannot be null");
        if (!Files.exists(path)) {
            return;
        }

        boolean wasLoading = loading;
        loading = true;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (rootElement != null && rootElement.isJsonObject()) {
                JsonObject rootObject = rootElement.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : rootObject.entrySet()) {
                    String subConfigName = entry.getKey();
                    JsonElement subElement = entry.getValue();
                    if (subElement.isJsonObject()) {
                        JsonObject subJsonObject = subElement.getAsJsonObject();
                        SubConfig subConfig = configs.get(subConfigName);
                        if (subConfig != null) {
                            subConfig.fromJson(subJsonObject, GSON);
                        } else {
                            unmappedConfigs.put(subConfigName, subJsonObject);
                        }
                    }
                }
            }
        } finally {
            loading = wasLoading;
        }
    }

    public synchronized void reload() throws IOException {
        load();
    }

    public synchronized void resetAll() {
        for (SubConfig subConfig : configs.values()) {
            subConfig.resetAll();
        }
    }

    public synchronized void clear() {
        configs.clear();
        unmappedConfigs.clear();
    }

    public static synchronized <T> void registerCodec(Class<T> clazz, ConfigCodec<T> codec) {
        Objects.requireNonNull(clazz, "Class cannot be null");
        Objects.requireNonNull(codec, "Codec cannot be null");
        codecs.put(clazz, codec);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> ConfigCodec<T> getCodec(Class<T> clazz) {
        if (clazz == null) return null;
        ConfigCodec<?> codec = codecs.get(clazz);
        if (codec != null) {
            return (ConfigCodec<T>) codec;
        }

        // Values are often stored as an implementation of their declared type
        // (for example, Path is a WindowsPath on Windows). Allow a codec
        // registered for an interface or superclass to handle those values.
        for (Map.Entry<Class<?>, ConfigCodec<?>> entry : codecs.entrySet()) {
            if (entry.getKey().isAssignableFrom(clazz)) {
                return (ConfigCodec<T>) entry.getValue();
            }
        }
        return null;
    }

    public static synchronized boolean hasCodec(Class<?> clazz) {
        return getCodec(clazz) != null;
    }

    public static synchronized void clearCodecs() {
        codecs.clear();
    }

    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new ConfigSerializableAdapterFactory())
                .setPrettyPrinting()
                .create();
    }
}
