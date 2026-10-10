// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.alerting.DeliveryLane;
import italy.mlsac.net.alerting.StaffMember;
import italy.mlsac.net.alerting.StaffRoster;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class FakeStaffRoster implements StaffRoster, DeliveryLane {

    public final List<FakeStaff> members = new ArrayList<>();

    public FakeStaff add(String name, boolean staff, boolean operator) {
        FakeStaff member = new FakeStaff(name, staff, operator);
        members.add(member);
        return member;
    }

    @Override
    public Optional<StaffMember> find(UUID player) {
        return members.stream().filter(member -> member.id().equals(player)).map(StaffMember.class::cast).findFirst();
    }

    @Override
    public List<StaffMember> online() {
        return members.stream().filter(FakeStaff::online).map(StaffMember.class::cast).toList();
    }

    @Override
    public void onPlayerThread(UUID player, Runnable task) {
        task.run();
    }

    @Override
    public void onMainThread(Runnable task) {
        task.run();
    }
}
