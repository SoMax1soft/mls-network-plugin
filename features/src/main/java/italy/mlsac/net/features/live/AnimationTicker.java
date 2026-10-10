// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.banimation.AnimationListener;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.penalty.AnimationStateChanged;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public final class AnimationTicker implements AnimationListener {

    private final TaskScheduler scheduler;
    private final EventBus bus;
    private final Map<UUID, TaskTicket> running = new ConcurrentHashMap<>();

    public AnimationTicker(TaskScheduler scheduler, EventBus bus) {
        this.scheduler = scheduler;
        this.bus = bus;
    }

    @Override
    public void started(UUID player, BooleanSupplier advance) {
        bus.publish(new AnimationStateChanged(player, true));
        TaskTicket ticket = scheduler.submit(TaskLane.following(player, () -> halt(player)),
                TaskTiming.repeating(0L, 1L), () -> {
                    if (!advance.getAsBoolean()) {
                        halt(player);
                    }
                });
        running.put(player, ticket);
    }

    @Override
    public void ended(UUID player) {
        halt(player);
        bus.publish(new AnimationStateChanged(player, false));
    }

    public void haltEvery() {
        running.values().forEach(TaskTicket::cancel);
        running.clear();
    }

    private void halt(UUID player) {
        TaskTicket ticket = running.remove(player);
        if (ticket != null) {
            ticket.cancel();
        }
    }
}
