// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.lifecycle;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerJoined;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class PerPlayerTickSource implements TickSource {

    private final TaskScheduler scheduler;
    private final EventBus bus;
    private final PlayerDirectory players;
    private final Map<UUID, TaskTicket> tasks = new ConcurrentHashMap<>();
    private final List<Subscription> subscriptions = new ArrayList<>();

    public PerPlayerTickSource(TaskScheduler scheduler, EventBus bus, PlayerDirectory players) {
        this.scheduler = scheduler;
        this.bus = bus;
        this.players = players;
    }

    @Override
    public void start() {
        subscriptions.add(bus.subscribe(PlayerJoined.class, joined -> begin(joined.player())));
        subscriptions.add(bus.subscribe(PlayerLeft.class, left -> end(left.player())));
        for (PlayerView player : players.online()) {
            begin(player.id());
        }
    }

    @Override
    public void stop() {
        subscriptions.forEach(Subscription::cancel);
        subscriptions.clear();
        tasks.values().forEach(TaskTicket::cancel);
        tasks.clear();
    }

    private void begin(UUID player) {
        AtomicLong counter = new AtomicLong();
        TaskTicket task = scheduler.submit(TaskLane.following(player, () -> tasks.remove(player)), TaskTiming.repeating(1L, 1L), () -> bus.publish(new PlayerTick(player, counter.incrementAndGet())));
        TaskTicket previous = tasks.put(player, task);
        if (previous != null) {
            previous.cancel();
        }
    }

    private void end(UUID player) {
        TaskTicket task = tasks.remove(player);
        if (task != null) {
            task.cancel();
        }
    }
}
