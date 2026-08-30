package com.mas6y6.musmeta.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class Registry<K,T> {
    private final Map<K, T> entries = new HashMap<>();
    private final String name;
    private boolean frozen;

    public Registry(String name) { this.name = name; }

    public void register(K id, T value) {
        if (frozen) throw new IllegalStateException("Registry '" + name + "' is frozen");
        T prev = entries.putIfAbsent(id, value);
        if (prev != null) throw new IllegalArgumentException("Duplicate id: " + id);
    }

    public T get(K id) { return entries.get(id); }

    public Stream<Map.Entry<K,T>> getAll() { return entries.entrySet().stream(); }

    void freeze() {
        frozen = true;
        entries.replaceAll((k, v) -> v); // no-op, but makes intent explicit
    }
}