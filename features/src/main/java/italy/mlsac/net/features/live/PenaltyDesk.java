// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.backend.BackendGateway;
import italy.mlsac.net.backend.request.AlertReport;
import italy.mlsac.net.backend.request.PunishReport;
import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.player.Audience;
import italy.mlsac.net.player.NoticeBoard;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.punishment.PenaltyClock;
import italy.mlsac.net.punishment.PenaltyServer;
import italy.mlsac.net.punishment.PunishEvent;
import italy.mlsac.net.punishment.StaffNotices;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.scoring.AlertEvent;
import italy.mlsac.net.scoring.DetectionReports;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public final class PenaltyDesk implements PenaltyServer, PenaltyClock, StaffNotices, DetectionReports {

    private static final String COOLDOWN_CLOCK = "violation.cooldown";
    private static final String STAMP_CLOCK = "penalty.stamp";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TaskScheduler scheduler;
    private final PlayerDirectory players;
    private final NoticeBoard notices;
    private final BackendGateway backend;
    private final Clock clock;
    private final ConsoleCommands console;

    public PenaltyDesk(TaskScheduler scheduler, PlayerDirectory players, NoticeBoard notices, BackendGateway backend,
                       Clock clock, ConsoleCommands console) {
        this.scheduler = scheduler;
        this.players = players;
        this.notices = notices;
        this.backend = backend;
        this.clock = clock;
        this.console = console;
    }

    @Override
    public void dispatchConsoleNextTick(String command) {
        scheduler.submit(TaskLane.MAIN, TaskTiming.immediately(), () -> console.dispatch(command));
    }

    @Override
    public boolean online(UUID player) {
        return players.byId(player).map(PlayerView::online).orElse(false);
    }

    @Override
    public void kickNextTick(UUID player, String reason) {
        scheduler.submit(TaskLane.following(player), TaskTiming.immediately(),
                () -> players.byId(player).filter(PlayerView::online).ifPresent(view -> view.kick(reason)));
    }

    @Override
    public void punishReport(PunishEvent event) {
        backend.sendPunishNotice(new PunishReport(event.player(), event.playerNick(), event.probability(),
                event.buffer(), event.offenceLevel(), event.action(), event.command()));
    }

    @Override
    public long wallMillisNow() {
        return clock.wallMillis(COOLDOWN_CLOCK);
    }

    @Override
    public String localTimeStamp() {
        return STAMP.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(clock.wallMillis(STAMP_CLOCK)));
    }

    @Override
    public void toAllStaff(String coloredLine) {
        notices.broadcast(Audience.STAFF, List.of(coloredLine));
    }

    @Override
    public void alert(AlertEvent event) {
        backend.sendAlertNotice(new AlertReport(event.player(), event.playerNick(), event.model(), event.probability(),
                event.buffer()));
    }
}
