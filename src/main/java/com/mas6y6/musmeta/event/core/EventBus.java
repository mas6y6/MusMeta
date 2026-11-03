package com.mas6y6.musmeta.event.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class EventBus implements AutoCloseable {
    private final Map<Class<? extends Event>, CopyOnWriteArrayList<RegisteredHandler>>
            handlers = new ConcurrentHashMap<>();

    private final AtomicLong sequence = new AtomicLong();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "musmeta-event-bus");
        thread.setDaemon(true);
        return thread;
    });

    public static final EventBus MAIN_BUS = new EventBus();

    private final Consumer<Throwable> exceptionHandler;

    private EventBus() {
        this(Throwable::printStackTrace);
    }

    public EventBus(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void register(EventListener eventListener) {
        register(null, eventListener);
    }

    public void register(Object owner, EventListener eventListener) {
        if (eventListener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        List<RegisteredHandler> registered = new ArrayList<>();

        Method[] methods = eventListener.getClass().getMethods();

        java.util.Arrays.sort(methods, Comparator.comparing(Method::toGenericString));

        for (Method method : methods) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);

            if (annotation == null) {
                continue;
            }

            if (Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 1
                    || !method.getReturnType().equals(void.class)
                    || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                throw new IllegalArgumentException(
                        "Invalid event handler method: " + method
                );
            }

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType =
                    (Class<? extends Event>) method.getParameterTypes()[0];

            registered.add(new RegisteredHandler(
                    owner,
                    eventListener,
                    method,
                    eventType,
                    annotation.priority(),
                    annotation.ignoreCancelled(),
                    sequence.getAndIncrement()
            ));
        }

        // Register only after every method has been validated.
        for (RegisteredHandler handler : registered) {
            handlers.computeIfAbsent(
                    handler.eventType,
                    ignored -> new CopyOnWriteArrayList<>()
            ).add(handler);
        }
    }

    public void unregister(EventListener eventListener) {
        if (eventListener == null) {
            return;
        }

        handlers.values().forEach(list ->
                list.removeIf(handler -> handler.eventListener == eventListener)
        );

        handlers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void unregisterOwner(Object owner) {
        if (owner == null) {
            return;
        }

        handlers.values().forEach(list ->
                list.removeIf(handler -> handler.owner == owner)
        );

        handlers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public <T extends Event> T post(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        List<RegisteredHandler> applicable = new ArrayList<>();

        handlers.forEach((eventType, registered) -> {
            if (eventType.isAssignableFrom(event.getClass())) {
                applicable.addAll(registered);
            }
        });

        applicable.sort(
                Comparator.comparing(RegisteredHandler::priority)
                        .thenComparingLong(RegisteredHandler::sequence)
        );

        for (RegisteredHandler handler : applicable) {
            if (handler.ignoreCancelled
                    && event instanceof Cancellable cancellable
                    && cancellable.isCancelled()) {
                continue;
            }

            try {
                handler.method.invoke(handler.eventListener, event);
            } catch (InvocationTargetException exception) {
                exceptionHandler.accept(exception.getCause());
            } catch (Throwable exception) {
                exceptionHandler.accept(exception);
            }
        }

        return event;
    }

    /**
     * Asynchronously dispatches an event using the event bus executor.
     */
    public CompletableFuture<Event> postAsync(Event event) {
        return CompletableFuture.supplyAsync(() -> post(event), executor);
    }

    public boolean hasListeners(Class<? extends Event> eventType) {
        return handlers.entrySet().stream()
                .anyMatch(entry ->
                        eventType.isAssignableFrom(entry.getKey())
                                && !entry.getValue().isEmpty()
                );
    }

    public void unregisterAll() {
        handlers.clear();
    }

    @Override
    public void close() {
        unregisterAll();
        executor.shutdown();
    }

    private record RegisteredHandler(
            Object owner,
            EventListener eventListener,
            Method method,
            Class<? extends Event> eventType,
            EventPriority priority,
            boolean ignoreCancelled,
            long sequence
    ) {
    }

    public static EventBus createNewEventBus() {
        return new EventBus();
    }
}