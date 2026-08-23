package com.mas6y6.musmeta.config;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ConfigContainer<T> {
    private final String name;
    private final T defaultValue;
    private T value;
    private final Type type;
    private final List<Consumer<T>> changeListeners = new ArrayList<>();

    public ConfigContainer(String name, T defaultValue) {
        this(name, defaultValue, defaultValue != null ? defaultValue.getClass() : Object.class);
    }

    public ConfigContainer(String name, T defaultValue, Type type) {
        this.name = Objects.requireNonNull(name, "ConfigContainer name cannot be null");
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.type = type != null ? type : (defaultValue != null ? defaultValue.getClass() : Object.class);
    }

    public ConfigContainer(String name, T defaultValue, Class<T> typeClass) {
        this(name, defaultValue, (Type) typeClass);
    }

    public ConfigContainer(String name, T defaultValue, TypeToken<T> typeToken) {
        this(name, defaultValue, typeToken != null ? typeToken.getType() : null);
    }

    @SuppressWarnings("unchecked")
    public ConfigContainer(String name, T defaultValue, ConfigCodec<T> codec) {
        this(name, defaultValue, defaultValue != null ? defaultValue.getClass() : Object.class);
        if (codec != null && defaultValue != null) {
            ConfigManager.registerCodec((Class<T>) defaultValue.getClass(), codec);
        }
    }

    public String getName() {
        return name;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        T oldValue = this.value;
        this.value = value;
        if (!Objects.equals(oldValue, value)) {
            notifyListeners(value);
        }
        ConfigManager.getInstance().saveQuietly();
    }

    public void set(T value) {
        setValue(value);
    }

    public T get() {
        return getValue();
    }

    public void reset() {
        setValue(defaultValue);
    }

    public Type getType() {
        return type;
    }

    public void addListener(Consumer<T> listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeListener(Consumer<T> listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners(T newValue) {
        for (Consumer<T> listener : changeListeners) {
            try {
                listener.accept(newValue);
            } catch (Exception e) {
                System.err.println("Error notifying config listener for '" + name + "': " + e.getMessage());
            }
        }
    }

    @Override
    public String toString() {
        return "ConfigContainer{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", defaultValue=" + defaultValue +
                '}';
    }
}
