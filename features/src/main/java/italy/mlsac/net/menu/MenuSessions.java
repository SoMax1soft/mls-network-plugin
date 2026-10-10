// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class MenuSessions {
    private final Map<UUID, OpenMenu> shown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> loadingTickets = new ConcurrentHashMap<>();
    private final AtomicLong ticketCounter = new AtomicLong();

    long awaitRender(UUID viewer) {
        long ticket = ticketCounter.incrementAndGet();
        loadingTickets.put(viewer, ticket);
        return ticket;
    }

    boolean finishLoading(UUID viewer, long ticket) {
        return loadingTickets.remove(viewer, ticket);
    }

    void show(UUID viewer, OpenMenu menu) {
        shown.put(viewer, menu);
    }

    Optional<OpenMenu> showing(UUID viewer) {
        return Optional.ofNullable(shown.get(viewer));
    }

    void hide(UUID viewer) {
        shown.remove(viewer);
    }

    boolean involves(UUID viewer) {
        return shown.containsKey(viewer) || loadingTickets.containsKey(viewer);
    }

    void drop(UUID viewer) {
        shown.remove(viewer);
        loadingTickets.remove(viewer);
    }
}
