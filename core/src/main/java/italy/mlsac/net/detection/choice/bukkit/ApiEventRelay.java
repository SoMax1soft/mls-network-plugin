// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice.bukkit;

import italy.mlsac.net.api.bukkit.ModelCheckEvent;
import italy.mlsac.net.api.bukkit.PlayerModelChangeEvent;
import italy.mlsac.net.api.bukkit.UltraFlagEvent;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.detection.choice.LegacyCheckObserved;
import italy.mlsac.net.detection.choice.ModelSelectionChanged;
import italy.mlsac.net.detection.choice.UltraFlagObserved;
import java.util.ArrayList;
import java.util.List;

public final class ApiEventRelay {

    private final EventBus bus;
    private final ApiEventSink sink;
    private final List<Subscription> subscriptions = new ArrayList<>();

    public ApiEventRelay(EventBus bus, ApiEventSink sink) {
        this.bus = bus;
        this.sink = sink;
    }

    public synchronized void start() {
        if (!subscriptions.isEmpty()) {
            return;
        }
        subscriptions.add(bus.subscribe(ModelSelectionChanged.class, changed ->
                sink.fire(new PlayerModelChangeEvent(changed.previous(), changed.current()))));
        subscriptions.add(bus.subscribe(LegacyCheckObserved.class, check ->
                sink.fire(new ModelCheckEvent(check.player(), check.playerNick(), check.model(), check.probability(),
                        check.buffer(), check.flagged()))));
        subscriptions.add(bus.subscribe(UltraFlagObserved.class, flag ->
                sink.fire(new UltraFlagEvent(flag.player(), flag.playerNick()))));
    }

    public synchronized void stop() {
        subscriptions.forEach(Subscription::cancel);
        subscriptions.clear();
    }
}
