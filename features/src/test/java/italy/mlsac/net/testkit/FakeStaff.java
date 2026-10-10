// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.alerting.StaffMember;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record FakeStaff(String name, boolean staff, boolean operator, List<String> received, List<String> sounds)
        implements StaffMember {

    public FakeStaff(String name, boolean staff, boolean operator) {
        this(name, staff, operator, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public UUID id() {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean online() {
        return true;
    }

    @Override
    public void send(String coloredText) {
        received.add(coloredText);
    }

    @Override
    public void playSound(String sound, float volume, float pitch) {
        sounds.add(sound);
    }
}
