// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import java.util.UUID;

public final class StaffEnrollment {

    public static final long JOIN_DELAY_TICKS = 20L;

    private final SubscriberSet alertSubscribers;
    private final SubscriberSet monitorSubscribers;
    private final StaffRoster staff;

    public StaffEnrollment(SubscriberSet alertSubscribers, SubscriberSet monitorSubscribers, StaffRoster staff) {
        this.alertSubscribers = alertSubscribers;
        this.monitorSubscribers = monitorSubscribers;
        this.staff = staff;
    }

    public void joinDelayElapsed(UUID player) {
        staff.find(player).filter(StaffMember::online).filter(StaffMember::staff)
                .ifPresent(member -> alertSubscribers.add(member.id()));
    }

    public void left(UUID player) {
        alertSubscribers.remove(player);
        monitorSubscribers.remove(player);
    }
}
