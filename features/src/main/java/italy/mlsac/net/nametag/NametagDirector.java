// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import italy.mlsac.net.workload.WorkloadCounters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class NametagDirector {

    static final int STAFF_CHECK_EVERY = 2;
    static final int FULL_PASS_EVERY = 20;

    private final Supplier<NametagSettings> settings;
    private final RendererChoice renderers;
    private final DestroyStyle destroyStyle;
    private final NametagText texts;
    private final SuspectTags tags;
    private final NametagPorts ports;
    private final WorkloadCounters counters;
    private final Map<UUID, ViewerSight> viewers = new ConcurrentHashMap<>();
    private final Map<UUID, Optional<GameRelease>> viewerClients = new ConcurrentHashMap<>();
    private NametagSettings applied;
    private long appearanceRevision;
    private long runs;

    public NametagDirector(Supplier<NametagSettings> settings, GameRelease server, NametagText texts,
                           SuspectTags tags, NametagPorts ports) {
        this(settings, server, texts, tags, ports, new WorkloadCounters());
    }

    public NametagDirector(Supplier<NametagSettings> settings, GameRelease server, NametagText texts,
                           SuspectTags tags, NametagPorts ports, WorkloadCounters counters) {
        this.settings = settings;
        this.counters = counters;
        this.renderers = new RendererChoice(server);
        this.destroyStyle = DestroyStyle.forServer(server);
        this.texts = texts;
        this.tags = tags;
        this.ports = ports;
    }

    public void joined(UUID player, int entityNumber, Optional<GameRelease> client, SuspectMode mode) {
        tags.register(player, entityNumber, mode);
        rebuildText(player, liveTagConfig());
        viewerClients.put(player, client);
        viewers.put(player, new ViewerSight(renderers.forViewer(client), destroyStyle, tags, ports.staff(player),
                entityNumber));
    }

    public TagDrawing drawingFor(UUID viewer) {
        return renderers.resolve(TagDrawing.AUTO, viewerClients.getOrDefault(viewer, Optional.empty()));
    }

    public TagDrawing drawWith(UUID viewer, TagDrawing wanted) {
        TagDrawing resolved = renderers.resolve(wanted, viewerClients.getOrDefault(viewer, Optional.empty()));
        ViewerSight previous = viewers.get(viewer);
        if (previous == null) {
            return resolved;
        }
        ViewerSight replacement = new ViewerSight(renderers.painter(resolved), destroyStyle, tags, false,
                previous.ownEntityNumber());
        viewers.put(viewer, replacement);
        ports.onViewerThread(viewer, () -> send(viewer, previous.destroyAll()));
        if (previous.staff()) {
            replacement.becomeStaff(ports.currentlyVisibleTo(viewer));
            NametagLook look = currentLook();
            ports.onViewerThread(viewer, () -> send(viewer, replacement.reconcile(look)));
        }
        return resolved;
    }

    public void entityChanged(UUID player, int entityNumber) {
        Optional<SuspectTags.Tag> old = tags.bySuspect(player);
        if (old.isPresent() && old.get().entityNumber() != entityNumber) {
            tags.register(player, entityNumber, old.get().mode());
            rebuildText(player, liveTagConfig());
            ViewerSight sight = viewers.get(player);
            if (sight != null) {
                ports.onViewerThread(player, () -> send(player, sight.renumberOwn(entityNumber)));
            }
        }
    }

    public void modeChanged(UUID player, SuspectMode mode) {
        if (tags.applyMode(player, mode)) {
            reconcileAll(new NametagLook(liveTagConfig(), appearanceRevision), ReconcileScope.CHANGES);
        }
    }

    public void left(UUID player) {
        tags.remove(player);
        viewers.remove(player);
        viewerClients.remove(player);
    }

    public Optional<ViewerSight> sight(UUID viewer) {
        return Optional.ofNullable(viewers.get(viewer));
    }

    public ViewerSight sightOrNull(UUID viewer) {
        return viewers.get(viewer);
    }

    public NametagLook currentLook() {
        return new NametagLook(liveTagConfig(), appearanceRevision);
    }

    public synchronized void periodic() {
        runs++;
        NametagSettings current = settings.get();
        boolean changed = false;
        boolean rebuildAll = false;
        if (!current.equals(applied)) {
            if (applied != null && !current.sameLook(applied)) {
                appearanceRevision++;
            }
            applied = current;
            changed = true;
            rebuildAll = true;
        }
        if (runs % STAFF_CHECK_EVERY == 0) {
            changed |= recheckStaff();
        }
        for (SuspectTags.Tag tag : tags.all()) {
            if (rebuildAll || ports.historyRevision(tag.suspect()) != tag.historyRevision()) {
                Optional<NametagPorts.HistoryView> history = ports.history(tag.suspect());
                long version = history.map(NametagPorts.HistoryView::version).orElse(Long.MIN_VALUE);
                counters.hologramTextBuilt();
                changed |= tags.updateText(tag.suspect(), version,
                        texts.build(history.map(NametagPorts.HistoryView::results).orElse(List.of()), current));
            }
        }
        NametagLook look = new NametagLook(current, appearanceRevision);
        placeOwnTags(look);
        boolean fullPass = runs % FULL_PASS_EVERY == 0;
        if (changed || fullPass) {
            reconcileAll(look, fullPass ? ReconcileScope.WITH_PASSENGER_REPAIR : ReconcileScope.CHANGES);
        } else {
            reconcileMissingOwnTags(look);
        }
    }

    public void stop() {
        for (Map.Entry<UUID, ViewerSight> viewer : viewers.entrySet()) {
            ports.onViewerThread(viewer.getKey(), () -> send(viewer.getKey(), viewer.getValue().destroyAll()));
        }
        viewers.clear();
        viewerClients.clear();
        tags.clear();
    }

    private boolean recheckStaff() {
        boolean changed = false;
        for (Map.Entry<UUID, ViewerSight> viewer : viewers.entrySet()) {
            boolean staffNow = ports.staff(viewer.getKey());
            if (staffNow && !viewer.getValue().staff()) {
                viewer.getValue().becomeStaff(ports.currentlyVisibleTo(viewer.getKey()));
                changed = true;
            } else if (!staffNow && viewer.getValue().staff()) {
                viewer.getValue().loseStaff();
                changed = true;
            }
        }
        return changed;
    }

    private void placeOwnTags(NametagLook look) {
        if (!look.ownTagShown()) {
            return;
        }
        for (Map.Entry<UUID, ViewerSight> viewer : viewers.entrySet()) {
            if (viewer.getValue().staff()) {
                ports.ownPlace(viewer.getKey()).ifPresent(viewer.getValue()::ownPlaced);
            }
        }
    }

    private void reconcileMissingOwnTags(NametagLook look) {
        if (!look.ownTagShown()) {
            return;
        }
        for (Map.Entry<UUID, ViewerSight> viewer : viewers.entrySet()) {
            ViewerSight sight = viewer.getValue();
            if (sight.missingOwnTag(look)) {
                ports.onViewerThread(viewer.getKey(), () -> send(viewer.getKey(), sight.reconcile(look)));
            }
        }
    }

    private void reconcileAll(NametagLook look, ReconcileScope scope) {
        for (Map.Entry<UUID, ViewerSight> viewer : viewers.entrySet()) {
            ViewerSight sight = viewer.getValue();
            if (!sight.staff() && sight.trackedCount() == 0) {
                continue;
            }
            ports.onViewerThread(viewer.getKey(), () -> send(viewer.getKey(), scope.packets(sight, look)));
        }
    }

    private void rebuildText(UUID player, NametagSettings current) {
        Optional<NametagPorts.HistoryView> history = ports.history(player);
        counters.hologramTextBuilt();
        tags.updateText(player, history.map(NametagPorts.HistoryView::version).orElse(Long.MIN_VALUE),
                texts.build(history.map(NametagPorts.HistoryView::results).orElse(List.of()), current));
    }

    private NametagSettings liveTagConfig() {
        NametagSettings current = applied;
        return current == null ? settings.get() : current;
    }

    private void send(UUID viewer, List<NametagPacket> packets) {
        if (!packets.isEmpty()) {
            ports.send(viewer, packets);
        }
    }

    private enum ReconcileScope {
        CHANGES {
            @Override
            List<NametagPacket> packets(ViewerSight sight, NametagLook look) {
                return sight.reconcile(look);
            }
        },
        WITH_PASSENGER_REPAIR {
            @Override
            List<NametagPacket> packets(ViewerSight sight, NametagLook look) {
                List<NametagPacket> packets = new ArrayList<>(sight.reconcile(look));
                packets.addAll(sight.repairPassengers());
                return packets;
            }
        };

        abstract List<NametagPacket> packets(ViewerSight sight, NametagLook look);
    }
}
