// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bus;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SynchronousEventBus implements EventBus {

    private final PluginLog log;
    private final Map<Class<?>, List<Consumer<Object>>> handlers = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Class<?>>> dispatchTypes = new ConcurrentHashMap<>();

    public SynchronousEventBus(PluginLog log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public <E> Subscription subscribe(Class<E> eventType, Consumer<? super E> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        Consumer<Object> erased = event -> handler.accept(eventType.cast(event));
        List<Consumer<Object>> list = handlers.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>());
        list.add(erased);
        return () -> list.remove(erased);
    }

    @Override
    public void publish(Object event) {
        Objects.requireNonNull(event, "event");
        for (Class<?> type : dispatchTypes.computeIfAbsent(event.getClass(), SynchronousEventBus::dispatchChain)) {
            List<Consumer<Object>> list = handlers.get(type);
            if (list != null) {
                for (Consumer<Object> handler : list) {
                    deliver(handler, event);
                }
            }
        }
    }

    private void deliver(Consumer<Object> handler, Object event) {
        try {
            handler.accept(event);
        } catch (RuntimeException failure) {
            log.error(LogTopic.GENERAL, "Event handler failed for " + event.getClass().getSimpleName()
                    + ": " + failure, failure);
        }
    }

    private static List<Class<?>> dispatchChain(Class<?> type) {
        Set<Class<?>> ordered = new LinkedHashSet<>();
        collect(type, ordered);
        return new ArrayList<>(ordered);
    }

    private static void collect(Class<?> type, Set<Class<?>> into) {
        if (type == null || type == Object.class || !into.add(type)) {
            return;
        }
        for (Class<?> implemented : type.getInterfaces()) {
            collect(implemented, into);
        }
        collect(type.getSuperclass(), into);
    }
}
