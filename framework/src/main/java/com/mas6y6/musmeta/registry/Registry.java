package com.mas6y6.musmeta.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public final class Registry<K,T> {
    private static final List<Registry<?, ?>> REGISTRIES = new CopyOnWriteArrayList<>();

    private Map<K, T> entries = new HashMap<>();
    private final String name;
    private boolean frozen;

    Registry(String name) {
        this.name = name;
        REGISTRIES.add(this);
    }

    public void register(K id, T value) {
        if (frozen) throw new IllegalStateException("Registry '" + name + "' is frozen");
        T prev = entries.putIfAbsent(id, value);
        if (prev != null) throw new IllegalArgumentException("Duplicate id: " + id);
    }

    public T get(K id) { return entries.get(id); }

    public Stream<Map.Entry<K,T>> getAll() { return entries.entrySet().stream(); }

    void freeze() {
        if (frozen) return;
        frozen = true;
        entries = Collections.unmodifiableMap(entries);
    }

    static List<Registry<?, ?>> all() {
        return List.copyOf(REGISTRIES);
    }
}