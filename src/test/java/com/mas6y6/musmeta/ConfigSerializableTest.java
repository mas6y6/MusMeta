package com.mas6y6.musmeta;

import com.google.gson.JsonParseException;
import com.mas6y6.musmeta.config.ConfigBuilder;
import com.mas6y6.musmeta.config.ConfigCodec;
import com.mas6y6.musmeta.config.ConfigContainer;
import com.mas6y6.musmeta.config.ConfigManager;
import com.mas6y6.musmeta.config.ConfigSerializable;
import com.mas6y6.musmeta.config.SubConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSerializableTest {

    @TempDir
    Path tempDir;

    private ConfigManager configManager;
    private Path tempConfigPath;

    @BeforeEach
    public void setUp() {
        configManager = ConfigManager.getInstance();
        configManager.clear();
        ConfigManager.clearCodecs();
        tempConfigPath = tempDir.resolve("config.json");
        configManager.setConfigPath(tempConfigPath);
    }

    @AfterEach
    public void tearDown() {
        configManager.clear();
        ConfigManager.clearCodecs();
        configManager.setConfigPath(null);
    }

    // 1. Custom class implementing ConfigSerializable with ConfigBuilder constructor
    public static class WindowConfig implements ConfigSerializable {
        private final int width;
        private final int height;
        private final boolean fullscreen;

        public WindowConfig(int width, int height, boolean fullscreen) {
            this.width = width;
            this.height = height;
            this.fullscreen = fullscreen;
        }

        public WindowConfig(ConfigBuilder builder) {
            this.width = builder.getInt("width", 800);
            this.height = builder.getInt("height", 600);
            this.fullscreen = builder.getBoolean("fullscreen", false);
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public boolean isFullscreen() {
            return fullscreen;
        }

        @Override
        public void serialize(ConfigBuilder builder) {
            builder.setInt("width", width)
                    .setInt("height", height)
                    .setBoolean("fullscreen", fullscreen);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WindowConfig that)) return false;
            return width == that.width && height == that.height && fullscreen == that.fullscreen;
        }

        @Override
        public int hashCode() {
            return Objects.hash(width, height, fullscreen);
        }
    }

    // 2. Custom class implementing ConfigSerializable with SubConfig constructor
    public static class AudioSettings implements ConfigSerializable {
        private final int volume;
        private final boolean muted;

        public AudioSettings(int volume, boolean muted) {
            this.volume = volume;
            this.muted = muted;
        }

        public AudioSettings(SubConfig config) {
            this.volume = config.getValue("volume", 100);
            this.muted = config.getValue("muted", false);
        }

        public int getVolume() {
            return volume;
        }

        public boolean isMuted() {
            return muted;
        }

        @Override
        public void serialize(SubConfig config) {
            config.setValue("volume", volume);
            config.setValue("muted", muted);
        }

        @Override
        public void serialize(ConfigBuilder builder) {
            builder.setInt("volume", volume);
            builder.setBoolean("muted", muted);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AudioSettings that)) return false;
            return volume == that.volume && muted == that.muted;
        }

        @Override
        public int hashCode() {
            return Objects.hash(volume, muted);
        }
    }

    // 3. Custom class with static configDeserialize(ConfigBuilder)
    public static class DatabaseEndpoint implements ConfigSerializable {
        private final String host;
        private final int port;

        public DatabaseEndpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Override
        public void serialize(ConfigBuilder builder) {
            builder.setString("host", host)
                    .setInt("port", port);
        }

        public static DatabaseEndpoint configDeserialize(ConfigBuilder builder) {
            return new DatabaseEndpoint(
                    builder.getString("host", "localhost"),
                    builder.getInt("port", 5432)
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DatabaseEndpoint that)) return false;
            return port == that.port && Objects.equals(host, that.host);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, port);
        }
    }

    // 4. Standalone POJO without ConfigSerializable, using ConfigCodec
    public static class ThemeProfile {
        private final String themeName;
        private final boolean darkMode;

        public ThemeProfile(String themeName, boolean darkMode) {
            this.themeName = themeName;
            this.darkMode = darkMode;
        }

        public String getThemeName() {
            return themeName;
        }

        public boolean isDarkMode() {
            return darkMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ThemeProfile that)) return false;
            return darkMode == that.darkMode && Objects.equals(themeName, that.themeName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(themeName, darkMode);
        }
    }

    // 5. Java Record
    public record Resolution(int width, int height) {}

    // 6. Invalid serializable without deserializer
    public static class InvalidSerializable implements ConfigSerializable {
        private final int x;

        public InvalidSerializable(int x) {
            this.x = x;
        }

        @Override
        public void serialize(ConfigBuilder builder) {
            builder.setInt("x", x);
        }
    }

    @Test
    public void testDirectSerializationAndDeserialization() {
        WindowConfig window = new WindowConfig(1920, 1080, true);
        String json = ConfigManager.GSON.toJson(window);
        assertTrue(json.contains("\"width\": 1920"));
        assertTrue(json.contains("\"height\": 1080"));
        assertTrue(json.contains("\"fullscreen\": true"));

        WindowConfig deserialized = ConfigManager.GSON.fromJson(json, WindowConfig.class);
        assertEquals(window, deserialized);
    }

    @Test
    public void testSubConfigConstructorDeserialization() {
        AudioSettings audio = new AudioSettings(75, false);
        String json = ConfigManager.GSON.toJson(audio);

        AudioSettings deserialized = ConfigManager.GSON.fromJson(json, AudioSettings.class);
        assertEquals(audio, deserialized);
    }

    @Test
    public void testStaticFactoryDeserialization() {
        DatabaseEndpoint endpoint = new DatabaseEndpoint("db.musmeta.org", 3306);
        String json = ConfigManager.GSON.toJson(endpoint);

        DatabaseEndpoint deserialized = ConfigManager.GSON.fromJson(json, DatabaseEndpoint.class);
        assertEquals(endpoint, deserialized);
    }

    @Test
    public void testConfigCodec() {
        ConfigCodec<ThemeProfile> codec = ConfigCodec.of(
                (profile, builder) -> builder.setString("name", profile.getThemeName())
                        .setBoolean("dark", profile.isDarkMode()),
                builder -> new ThemeProfile(
                        builder.getString("name", "Light"),
                        builder.getBoolean("dark", false)
                )
        );

        ConfigManager.registerCodec(ThemeProfile.class, codec);

        ThemeProfile profile = new ThemeProfile("Dracula", true);
        String json = ConfigManager.GSON.toJson(profile);
        assertTrue(json.contains("\"name\": \"Dracula\""));
        assertTrue(json.contains("\"dark\": true"));

        ThemeProfile deserialized = ConfigManager.GSON.fromJson(json, ThemeProfile.class);
        assertEquals(profile, deserialized);
    }

    @Test
    public void testRecordSerializationAndDeserialization() {
        Resolution resolution = new Resolution(2560, 1440);
        ConfigBuilder builder = new ConfigBuilder();
        builder.setInt("width", 2560);
        builder.setInt("height", 1440);

        Resolution reconstructed = builder.build(Resolution.class);
        assertEquals(resolution, reconstructed);
    }

    @Test
    public void testSubConfigDirectToClassAndFromClass() {
        SubConfig subConfig = configManager.registerConfig("window");
        WindowConfig original = new WindowConfig(1440, 900, false);

        // Convert class directly to SubConfig
        subConfig.fromClass(original);

        // Convert SubConfig directly to class
        WindowConfig reconstructed = subConfig.toClass(WindowConfig.class);
        assertEquals(original, reconstructed);
    }

    @Test
    public void testSubConfigSaveAndLoadWithConfigSerializable() throws IOException {
        SubConfig uiConfig = configManager.registerConfig("ui");
        ConfigContainer<WindowConfig> windowContainer = uiConfig.register("window", new WindowConfig(800, 600, false));

        windowContainer.setValue(new WindowConfig(1920, 1080, true));

        configManager.save();
        assertTrue(Files.exists(tempConfigPath));

        String content = Files.readString(tempConfigPath);
        assertTrue(content.contains("\"width\": 1920"));
        assertTrue(content.contains("\"fullscreen\": true"));

        // Clear and reload
        configManager.clear();
        SubConfig reloadedUiConfig = configManager.registerConfig("ui");
        ConfigContainer<WindowConfig> reloadedWindow = reloadedUiConfig.register("window", new WindowConfig(800, 600, false));

        configManager.load();

        assertEquals(new WindowConfig(1920, 1080, true), reloadedWindow.getValue());
    }

    @Test
    public void testLoadBeforeRegisterCustomClass() throws IOException {
        String json = """
                {
                  "system": {
                    "window": {
                      "width": 3840,
                      "height": 2160,
                      "fullscreen": true
                    }
                  }
                }
                """;
        Files.writeString(tempConfigPath, json);

        configManager.load();

        SubConfig systemConfig = configManager.registerConfig("system");
        ConfigContainer<WindowConfig> container = systemConfig.register("window", new WindowConfig(800, 600, false));

        assertEquals(new WindowConfig(3840, 2160, true), container.getValue());
    }

    @Test
    public void testNullHandling() {
        WindowConfig nullConfig = null;
        String json = ConfigManager.GSON.toJson(nullConfig, WindowConfig.class);
        assertEquals("null", json);

        WindowConfig deserialized = ConfigManager.GSON.fromJson("null", WindowConfig.class);
        assertNull(deserialized);
    }

    @Test
    public void testInvalidSerializableThrowsException() {
        InvalidSerializable invalid = new InvalidSerializable(99);
        String json = ConfigManager.GSON.toJson(invalid);

        assertThrows(JsonParseException.class, () -> {
            ConfigManager.GSON.fromJson(json, InvalidSerializable.class);
        });
    }
}
