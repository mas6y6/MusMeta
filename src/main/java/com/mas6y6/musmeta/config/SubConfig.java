package com.mas6y6.musmeta.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SubConfig {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SubConfig.class);

    private final String name;
    private final Map<String, ConfigContainer<?>> containers = new LinkedHashMap<>();
    private final Map<String, JsonElement> unmappedJson = new LinkedHashMap<>();

    public SubConfig(String name) {
        this.name = Objects.requireNonNull(name, "SubConfig name cannot be null");
    }

    public String getName() {
        return name;
    }

    public <T> ConfigContainer<T> register(String name, T defaultValue) {
        ConfigContainer<T> container = new ConfigContainer<>(name, defaultValue);
        return register(container);
    }

    public <T> ConfigContainer<T> register(String name, T defaultValue, Class<T> typeClass) {
        ConfigContainer<T> container = new ConfigContainer<>(name, defaultValue, typeClass);
        return register(container);
    }

    public <T> ConfigContainer<T> register(String name, T defaultValue, Type type) {
        ConfigContainer<T> container = new ConfigContainer<>(name, defaultValue, type);
        return register(container);
    }

    public <T> ConfigContainer<T> register(String name, T defaultValue, TypeToken<T> typeToken) {
        ConfigContainer<T> container = new ConfigContainer<>(name, defaultValue, typeToken);
        return register(container);
    }

    public <T> ConfigContainer<T> register(String name, T defaultValue, ConfigCodec<T> codec) {
        if (codec != null && defaultValue != null) {
            ConfigManager.registerCodec((Class<T>) defaultValue.getClass(), codec);
        }
        return register(name, defaultValue);
    }

    public <T> ConfigContainer<T> register(ConfigContainer<T> container) {
        Objects.requireNonNull(container, "ConfigContainer cannot be null");
        String key = container.getName();
        containers.put(key, container);

        // If JSON was previously loaded before registering this container, apply it now
        if (unmappedJson.containsKey(key)) {
            JsonElement jsonElement = unmappedJson.remove(key);
            try {
                T deserialized = ConfigManager.GSON.fromJson(jsonElement, container.getType());
                ConfigManager configManager = ConfigManager.getInstance();
                boolean wasLoading = configManager.isLoading();
                configManager.setLoading(true);
                try {
                    container.setValue(deserialized);
                } finally {
                    configManager.setLoading(wasLoading);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to deserialize property '" + key + "' in subconfig '" + name + "': " + e);
            }
        }
        return container;
    }

    @SuppressWarnings("unchecked")
    public <T> ConfigContainer<T> getContainer(String name) {
        return (ConfigContainer<T>) containers.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String name) {
        ConfigContainer<?> container = containers.get(name);
        if (container != null) {
            return (T) container.getValue();
        }
        if (unmappedJson.containsKey(name)) {
            JsonElement element = unmappedJson.get(name);
            if (element != null && !element.isJsonNull()) {
                if (element.isJsonPrimitive()) {
                    com.google.gson.JsonPrimitive primitive = element.getAsJsonPrimitive();
                    if (primitive.isBoolean()) return (T) Boolean.valueOf(primitive.getAsBoolean());
                    if (primitive.isNumber()) return (T) primitive.getAsNumber();
                    if (primitive.isString()) return (T) primitive.getAsString();
                }
                return (T) ConfigManager.GSON.fromJson(element, Object.class);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String name, T defaultValue) {
        ConfigContainer<?> container = containers.get(name);
        if (container != null && container.getValue() != null) {
            return (T) container.getValue();
        }
        if (unmappedJson.containsKey(name)) {
            JsonElement element = unmappedJson.get(name);
            if (element != null && !element.isJsonNull()) {
                if (defaultValue != null) {
                    try {
                        return (T) ConfigManager.GSON.fromJson(element, defaultValue.getClass());
                    } catch (Exception ignored) {}
                }
                if (element.isJsonPrimitive()) {
                    com.google.gson.JsonPrimitive primitive = element.getAsJsonPrimitive();
                    if (primitive.isBoolean()) return (T) Boolean.valueOf(primitive.getAsBoolean());
                    if (primitive.isNumber()) {
                        if (defaultValue instanceof Integer) return (T) Integer.valueOf(primitive.getAsInt());
                        if (defaultValue instanceof Long) return (T) Long.valueOf(primitive.getAsLong());
                        if (defaultValue instanceof Double) return (T) Double.valueOf(primitive.getAsDouble());
                        if (defaultValue instanceof Float) return (T) Float.valueOf(primitive.getAsFloat());
                        return (T) primitive.getAsNumber();
                    }
                    if (primitive.isString()) return (T) primitive.getAsString();
                }
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <T> void setValue(String name, T value) {
        ConfigContainer<T> container = (ConfigContainer<T>) containers.get(name);
        if (container != null) {
            container.setValue(value);
        } else {
            unmappedJson.put(name, ConfigManager.GSON.toJsonTree(value));
            ConfigManager.getInstance().saveQuietly();
        }
    }

    public boolean has(String name) {
        return containers.containsKey(name) || unmappedJson.containsKey(name);
    }

    public Map<String, ConfigContainer<?>> getContainers() {
        return Collections.unmodifiableMap(containers);
    }

    public void resetAll() {
        for (ConfigContainer<?> container : containers.values()) {
            container.reset();
        }
    }

    public JsonObject toJson(Gson gson) {
        JsonObject jsonObject = new JsonObject();
        // Preserve unmapped values
        for (Map.Entry<String, JsonElement> entry : unmappedJson.entrySet()) {
            jsonObject.add(entry.getKey(), entry.getValue());
        }
        // Add all registered container values
        for (Map.Entry<String, ConfigContainer<?>> entry : containers.entrySet()) {
            ConfigContainer<?> container = entry.getValue();
            JsonElement element = gson.toJsonTree(container.getValue(), container.getType());
            jsonObject.add(entry.getKey(), element);
        }
        return jsonObject;
    }

    public void fromJson(JsonObject jsonObject, Gson gson) {
        if (jsonObject == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            JsonElement valueElement = entry.getValue();
            ConfigContainer<?> container = containers.get(key);
            if (container != null) {
                try {
                    Object value = gson.fromJson(valueElement, container.getType());
                    setContainerRawValue(container, value);
                } catch (Exception e) {
                    LOGGER.error("Failed to deserialize property '" + key + "' in subconfig '" + name + "': " + e.getMessage());
                }
            } else {
                unmappedJson.put(key, valueElement);
            }
        }
        ConfigManager.getInstance().saveQuietly();
    }

    @SuppressWarnings("unchecked")
    private <T> void setContainerRawValue(ConfigContainer<T> container, Object value) {
        container.setValue((T) value);
    }

    public ConfigBuilder toBuilder() {
        return ConfigBuilder.from(this);
    }

    public void fromBuilder(ConfigBuilder builder) {
        if (builder != null) {
            builder.applyTo(this);
        }
    }

    public <T> T toClass(Class<T> clazz) {
        return toBuilder().build(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> void fromClass(T instance) {
        if (instance == null) {
            return;
        }
        ConfigBuilder builder = new ConfigBuilder();
        if (instance instanceof ConfigSerializable serializable) {
            serializable.serialize(builder);
        } else if (ConfigManager.hasCodec(instance.getClass())) {
            ConfigCodec<T> codec = (ConfigCodec<T>) ConfigManager.getCodec(instance.getClass());
            codec.encode(instance, builder);
        } else {
            JsonElement tree = ConfigManager.GSON.toJsonTree(instance);
            if (tree.isJsonObject()) {
                builder = ConfigBuilder.from(tree.getAsJsonObject());
            }
        }
        fromBuilder(builder);
    }
}
