// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.logbook.Logbook;

public final class CrossServerFeed {

    static final int REMEMBERED_IDS = 2048;

    private final AlertTexts texts;
    private final ModelNames models;
    private final StaffRoster staff;
    private final DeliveryLane lane;
    private final Logbook log;
    private final RecentEventIds seen = new RecentEventIds(REMEMBERED_IDS);

    public CrossServerFeed(AlertTexts texts, ModelNames models, StaffRoster staff, DeliveryLane lane, Logbook log) {
        this.texts = texts;
        this.models = models;
        this.staff = staff;
        this.lane = lane;
        this.log = log;
    }

    public void receive(RemoteEvent event) {
        if (seen.firstSighting(event.eventId())) {
            RemoteServerFlag.from(event).ifPresent(this::announce);
        }
    }

    private void announce(RemoteServerFlag alert) {
        String line = texts.crossServer(alert, models.displayName(alert.model()));
        staff.online().forEach(member -> lane.onPlayerThread(member.id(), () -> deliver(member, line)));
        lane.onMainThread(() -> log.info(texts.strip(line)));
    }

    private void deliver(StaffMember member, String line) {
        staff.find(member.id())
                .filter(StaffMember::online)
                .filter(StaffMember::staffOrOp)
                .ifPresent(target -> target.send(line));
    }
}
