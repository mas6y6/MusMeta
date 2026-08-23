package com.mas6y6.musmeta.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Function;

/**
 * A builder providing fluent and structured reading/writing of configuration properties.
 * Can be converted directly to and from {@link SubConfig}, JSON objects, and custom class instances.
 */
public class ConfigBuilder {
    private final JsonObject jsonObject;

    public ConfigBuilder() {
        this.jsonObject = new JsonObject();
    }

    public ConfigBuilder(JsonObject jsonObject) {
        this.jsonObject = jsonObject != null ? jsonObject.deepCopy() : new JsonObject();
    }

    public ConfigBuilder(SubConfig subConfig) {
        this.jsonObject = subConfig != null ? subConfig.toJson(ConfigManager.GSON) : new JsonObject();
    }

    public static ConfigBuilder create() {
        return new ConfigBuilder();
    }

    public static ConfigBuilder from(JsonObject jsonObject) {
        return new ConfigBuilder(jsonObject);
    }

    public static ConfigBuilder from(SubConfig subConfig) {
        return new ConfigBuilder(subConfig);
    }

    // Setters

    public ConfigBuilder set(String key, Object value) {
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

    public ConfigBuilder setString(String key, String value) {
        return set(key, value);
    }

    public ConfigBuilder setInt(String key, int value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public ConfigBuilder setLong(String key, long value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public ConfigBuilder setDouble(String key, double value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public ConfigBuilder setBoolean(String key, boolean value) {
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

    public <T> T get(String key, Class<T> type) {
        JsonElement element = jsonObject.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return ConfigManager.GSON.fromJson(element, type);
    }

    public <T> T get(String key, Class<T> type, T defaultValue) {
        T val = get(key, type);
        return val != null ? val : defaultValue;
    }

    public <T> T get(String key, TypeToken<T> typeToken) {
        JsonElement element = jsonObject.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return ConfigManager.GSON.fromJson(element, typeToken.getType());
    }

    public <T> T get(String key, Type type) {
        JsonElement element = jsonObject.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return ConfigManager.GSON.fromJson(element, type);
    }

    public ConfigBuilder getBuilder(String key) {
        JsonElement element = jsonObject.get(key);
        if (element != null && element.isJsonObject()) {
            return new ConfigBuilder(element.getAsJsonObject());
        }
        return new ConfigBuilder();
    }

    public JsonObject toJsonObject() {
        return jsonObject.deepCopy();
    }

    public void applyTo(SubConfig subConfig) {
        if (subConfig == null) return;
        subConfig.fromJson(jsonObject, ConfigManager.GSON);
    }

    public SubConfig toSubConfig(String name) {
        SubConfig subConfig = new SubConfig(name);
        applyTo(subConfig);
        return subConfig;
    }

    public SubConfig toSubConfig() {
        return toSubConfig("anonymous");
    }

    /**
     * Reconstructs an instance of {@code clazz} from this builder's properties.
     */
    public <T> T build(Class<T> clazz) {
        return ConfigSerializableAdapterFactory.deserialize(clazz, this);
    }

    /**
     * Builds an instance using a custom factory function.
     */
    public <T> T build(Function<ConfigBuilder, T> factory) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        return factory.apply(this);
    }

    @Override
    public String toString() {
        return jsonObject.toString();
    }
}
