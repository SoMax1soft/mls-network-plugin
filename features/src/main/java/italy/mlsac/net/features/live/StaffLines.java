// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.alerting.SubscriberBroadcast;
import italy.mlsac.net.alerting.SubscriberSet;
import italy.mlsac.net.punishment.StaffNotices;
import italy.mlsac.net.staff.StaffChannel;

public final class StaffLines implements StaffChannel {

    private final SubscriberBroadcast broadcast;
    private final SubscriberSet alertSubscribers;
    private final StaffNotices staff;

    public StaffLines(SubscriberBroadcast broadcast, SubscriberSet alertSubscribers, StaffNotices staff) {
        this.broadcast = broadcast;
        this.alertSubscribers = alertSubscribers;
        this.staff = staff;
    }

    @Override
    public void toAlertSubscribers(String coloredLine) {
        broadcast.quietly(alertSubscribers, coloredLine);
    }

    @Override
    public void toAllStaff(String coloredLine) {
        staff.toAllStaff(coloredLine);
    }
}
