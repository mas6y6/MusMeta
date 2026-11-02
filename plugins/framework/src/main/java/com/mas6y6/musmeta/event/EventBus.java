package com.mas6y6.musmeta.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class EventBus {

    private final Map<String, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "musmeta-event-bus");
        thread.setDaemon(true);
        return thread;
    });

    public void register(String event, Consumer<Object> listener) {
        if (event == null || event.isBlank() || listener == null) {
            return;
        }
        listeners.computeIfAbsent(event, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void unregister(String event, Consumer<Object> listener) {
        List<Consumer<Object>> list = listeners.get(event);
        if (list != null && listener != null) {
            list.remove(listener);
        }
    }

    public void post(String event, Object payload) {
        List<Consumer<Object>> list = listeners.get(event);
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Consumer<Object>> snapshot = List.copyOf(list);
        executor.submit(() -> {
            for (Consumer<Object> listener : snapshot) {
                try {
                    listener.accept(payload);
                } catch (Throwable ignored) {
                    // Keep other listeners running even if one fails.
                }
            }
        });
    }

    public boolean hasListeners(String event) {
        List<Consumer<Object>> list = listeners.get(event);
        return list != null && !list.isEmpty();
    }

    public void shutdown() {
        executor.shutdown();
    }
}