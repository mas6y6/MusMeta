package com.mas6y6.musmeta.plugin.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MusMetaBlackboard implements IGlobalPropertyService {

    private final Map<String, Key> keys = new ConcurrentHashMap<>();
    private final Map<IPropertyKey, Object> values = new ConcurrentHashMap<>();

    @Override
    public IPropertyKey resolveKey(String name) {
        return keys.computeIfAbsent(name, Key::new);
    }

    @Override
    public <T> T getProperty(IPropertyKey key) {
        return getProperty(key, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        Object value = values.get(key);
        return value != null ? (T) value : defaultValue;
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = values.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static final class Key implements IPropertyKey {

        private final String name;

        Key(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}