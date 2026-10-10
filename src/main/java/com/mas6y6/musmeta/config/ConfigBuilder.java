package com.mas6y6.musmeta.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Function;

public class ConfigBuilder {
    private final JsonObject jsonObject;

    public ConfigBuilder() {
        this.jsonObject = new JsonObject();
    }

    public ConfigBuilder(@Nullable JsonObject jsonObject) {
        this.jsonObject = jsonObject != null ? jsonObject.deepCopy() : new JsonObject();
    }

    public ConfigBuilder(@Nullable SubConfig subConfig) {
        this.jsonObject = subConfig != null ? subConfig.toJson(ConfigManager.GSON) : new JsonObject();
    }

    public static @NonNull ConfigBuilder create() {
        return new ConfigBuilder();
    }

    public static @NonNull ConfigBuilder from(JsonObject jsonObject) {
        return new ConfigBuilder(jsonObject);
    }

    public static @NonNull ConfigBuilder from(SubConfig subConfig) {
        return new ConfigBuilder(subConfig);
    }

    // Setters

    public @NonNull ConfigBuilder set(@NonNull String key, @Nullable Object value) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (value == null) {
            jsonObject.add(key, JsonNull.INSTANCE);
        } else if (value instanceof ConfigSerializable serializable) {
            ConfigBuilder childBuilder = new ConfigBuilder();
            serializable.serialize(childBuilder);
            jsonObject.add(key, childBuilder.toJsonObject());
        } else if (value instanceof ConfigBuilder childBuilder) {
            jsonObject.add(key, childBuilder.toJsonObject());
        } else if (value instanceof JsonElement element) {
            jsonObject.add(key, element);
        } else {
            jsonObject.add(key, ConfigManager.GSON.toJsonTree(value));
        }
        return this;
    }

    public ConfigBuilder setString(@NonNull String key, String value) {
        return set(key, value);
    }

    public @NonNull ConfigBuilder setInt(@NonNull String key, int value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public @NonNull ConfigBuilder setLong(@NonNull String key, long value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public @NonNull ConfigBuilder setDouble(@NonNull String key, double value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public @NonNull ConfigBuilder setBoolean(@NonNull String key, boolean value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    // Getters

    public boolean has(String key) {
        return jsonObject.has(key) && !jsonObject.get(key).isJsonNull();
    }

    public JsonElement get(String key) {
        return jsonObject.get(key);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            return element.getAsString();
        }
        return defaultValue;
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            try {
                return element.getAsInt();
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            try {
                return element.getAsLong();
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double defaultValue) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            try {
                return element.getAsDouble();
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            try {
                return element.getAsBoolean();
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public <T> @Nullable T get(String key, @NonNull Class<T> type) {
        JsonElement element = jsonObject.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return ConfigManager.GSON.fromJson(element, type);
    }

    public <T> T get(String key, @NonNull Class<T> type, T defaultValue) {
        T val = get(key, type);
        return val != null ? val : defaultValue;
    }

    public <T> @Nullable T get(String key, @NonNull TypeToken<T> typeToken) {
        JsonElement element = jsonObject.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return ConfigManager.GSON.fromJson(element, typeToken.getType());
    }

    public <T> @Nullable T get(String key, @NonNull Type type) {
        JsonElement element = jsonObject.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return ConfigManager.GSON.fromJson(element, type);
    }

    public @NonNull ConfigBuilder getBuilder(String key) {
        JsonElement element = jsonObject.get(key);
        if (element != null && element.isJsonObject()) {
            return new ConfigBuilder(element.getAsJsonObject());
        }
        return new ConfigBuilder();
    }

    public @NonNull JsonObject toJsonObject() {
        return jsonObject.deepCopy();
    }

    public void applyTo(@Nullable SubConfig subConfig) {
        if (subConfig == null) return;
        subConfig.fromJson(jsonObject, ConfigManager.GSON);
    }

    public @NonNull SubConfig toSubConfig(String name) {
        SubConfig subConfig = new SubConfig(name);
        applyTo(subConfig);
        return subConfig;
    }

    public SubConfig toSubConfig() {
        return toSubConfig("anonymous");
    }


    public <T> T build(Class<T> clazz) {
        return ConfigSerializableAdapterFactory.deserialize(clazz, this);
    }

    public <T> T build(@NonNull Function<ConfigBuilder, T> factory) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        return factory.apply(this);
    }

    @Override
    public String toString() {
        return jsonObject.toString();
    }
}
