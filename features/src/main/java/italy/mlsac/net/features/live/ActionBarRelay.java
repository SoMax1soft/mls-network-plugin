// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.alerting.ActionBarPorts;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.SuspectRoster;
import italy.mlsac.net.scoring.SuspectSnapshot;
import italy.mlsac.net.scoring.ViolationLedger;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public final class ActionBarRelay implements ActionBarPorts {

    private final PlayerDirectory players;
    private final TaskScheduler scheduler;
    private final SuspectRoster roster;
    private final ViolationLedger ledger;

    public ActionBarRelay(PlayerDirectory players, TaskScheduler scheduler, SuspectRoster roster,
                          ViolationLedger ledger) {
        this.players = players;
        this.scheduler = scheduler;
        this.roster = roster;
        this.ledger = ledger;
    }

    @Override
    public boolean online(UUID player) {
        return players.byId(player).map(PlayerView::online).orElse(false);
    }

    @Override
    public void showHotbarText(UUID viewer, String coloredText) {
        players.byId(viewer).filter(PlayerView::online).ifPresent(view -> view.sendActionBar(coloredText));
    }

    @Override
    public Optional<SuspectSnapshot> snapshot(UUID target) {
        return roster.active(target).filter(SuspectRecord::hasChecks).map(SuspectRecord::snapshot);
    }

    @Override
    public int offenceLevel(UUID target) {
        return ledger.level(target);
    }

    @Override
    public void repeatOnViewerThread(UUID viewer, long everyTicks, BooleanSupplier runWhileTrue) {
        if (!runWhileTrue.getAsBoolean()) {
            return;
        }
        AtomicReference<TaskTicket> ticket = new AtomicReference<>();
        ticket.set(scheduler.submit(TaskLane.following(viewer, () -> cancel(ticket)),
                TaskTiming.repeating(everyTicks, everyTicks), () -> {
                    if (!runWhileTrue.getAsBoolean()) {
                        cancel(ticket);
                    }
                }));
    }

    private static void cancel(AtomicReference<TaskTicket> ticket) {
        TaskTicket current = ticket.get();
        if (current != null) {
            current.cancel();
        }
    }
}
