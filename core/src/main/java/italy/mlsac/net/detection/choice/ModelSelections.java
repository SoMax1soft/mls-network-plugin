// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice;

import italy.mlsac.net.api.FallbackReason;
import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.api.ModelSelection;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.config.ConfigApplied;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.player.PlayerLeft;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ModelSelections implements ModelSelectionDesk {

    private final Supplier<EffectiveConfig> config;
    private final EventBus bus;
    private final PluginLog log;
    private final Map<UUID, ModelSelection> overrides = new ConcurrentHashMap<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private volatile ModelAvailability availability;
    private volatile Map<ModelRoute, Integer> playersPerRoute = Map.of();

    public ModelSelections(Supplier<EffectiveConfig> config, EventBus bus, PluginLog log) {
        this.config = Objects.requireNonNull(config, "config");
        this.bus = Objects.requireNonNull(bus, "bus");
        this.log = Objects.requireNonNull(log, "log");
        this.availability = ModelAvailability.of(config.get().detection());
    }

    public synchronized void start() {
        if (!subscriptions.isEmpty()) {
            return;
        }
        subscriptions.add(bus.subscribe(ConfigApplied.class, applied -> configurationRebuilt(applied.current())));
        subscriptions.add(bus.subscribe(PlayerLeft.class, left -> forget(left.player())));
        configurationRebuilt(config.get());
    }

    public synchronized void stop() {
        subscriptions.forEach(Subscription::cancel);
        subscriptions.clear();
        overrides.clear();
        recount();
    }

    @Override
    public ModelSelection set(UUID player, ModelChoice choice) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(choice, "choice");
        ModelSelection previous;
        ModelSelection next;
        synchronized (this) {
            previous = get(player);
            next = resolve(player, choice, availability);
            store(next);
            recount();
        }
        announce(previous, next);
        return next;
    }

    @Override
    public ModelSelection get(UUID player) {
        ModelSelection known = overrides.get(player);
        return known != null ? known : ModelSelection.serverDefault(player);
    }

    @Override
    public void clear(UUID player) {
        set(player, ModelChoice.DEFAULT);
    }

    @Override
    public Set<ModelChoice> available() {
        return availability.available();
    }

    @Override
    public Optional<FallbackReason> unavailableBecause(ModelChoice choice) {
        return availability.blocker(choice);
    }

    @Override
    public ModelRoute routeOf(UUID player) {
        if (overrides.isEmpty()) {
            return ModelRoute.SERVER_DEFAULT;
        }
        ModelSelection known = overrides.get(player);
        return known == null ? ModelRoute.SERVER_DEFAULT : ModelRoute.of(known.effective());
    }

    @Override
    public boolean anyPlayerOn(ModelRoute route) {
        return playersPerRoute.getOrDefault(route, 0) > 0;
    }

    @Override
    public void refused(ModelChoice model, FallbackReason reason) {
        if (model == ModelChoice.DEFAULT) {
            return;
        }
        List<ModelSelectionChanged> changes;
        synchronized (this) {
            if (availability.refuses(model)) {
                return;
            }
            availability = availability.withRefusal(model, reason);
            changes = resolveEveryone();
        }
        log.info(LogTopic.DETECTION, "Model choice " + model.id() + " is not available any more (" + reason
                + "); players who chose it use the server default until the configuration is rebuilt");
        changes.forEach(bus::publish);
    }

    private void configurationRebuilt(EffectiveConfig current) {
        List<ModelSelectionChanged> changes;
        synchronized (this) {
            availability = ModelAvailability.of(current.detection());
            changes = resolveEveryone();
        }
        changes.forEach(bus::publish);
    }

    private void forget(UUID player) {
        synchronized (this) {
            overrides.remove(player);
            recount();
        }
    }

    private List<ModelSelectionChanged> resolveEveryone() {
        List<ModelSelectionChanged> changes = new ArrayList<>();
        for (ModelSelection previous : List.copyOf(overrides.values())) {
            ModelSelection next = resolve(previous.player(), previous.requested(), availability);
            if (!next.equals(previous)) {
                store(next);
                changes.add(new ModelSelectionChanged(previous, next));
            }
        }
        recount();
        return changes;
    }

    private static ModelSelection resolve(UUID player, ModelChoice choice, ModelAvailability open) {
        if (choice == ModelChoice.DEFAULT) {
            return ModelSelection.serverDefault(player);
        }
        return open.blocker(choice)
                .map(reason -> ModelSelection.fellBack(player, choice, reason))
                .orElseGet(() -> ModelSelection.granted(player, choice));
    }

    private void store(ModelSelection selection) {
        if (selection.requested() == ModelChoice.DEFAULT) {
            overrides.remove(selection.player());
        } else {
            overrides.put(selection.player(), selection);
        }
    }

    private void recount() {
        EnumMap<ModelRoute, Integer> counts = new EnumMap<>(ModelRoute.class);
        for (ModelSelection selection : overrides.values()) {
            counts.merge(ModelRoute.of(selection.effective()), 1, Integer::sum);
        }
        playersPerRoute = counts;
    }

    private void announce(ModelSelection previous, ModelSelection next) {
        if (!next.equals(previous)) {
            bus.publish(new ModelSelectionChanged(previous, next));
        }
    }
}
