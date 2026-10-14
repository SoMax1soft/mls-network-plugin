// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.integrity;

import italy.mlsac.net.detection.BadPacketKind;
import italy.mlsac.net.scoring.DetectionFlow;
import italy.mlsac.net.scoring.IntegrityVerdict;
import italy.mlsac.net.scoring.SuspectRoster;
import java.util.UUID;

public final class BadPacketStrike {

    private final SuspectRoster roster;
    private final DetectionFlow flow;
    private final IntegrityAudience audience;

    public BadPacketStrike(SuspectRoster roster, DetectionFlow flow, IntegrityAudience audience) {
        this.roster = roster;
        this.flow = flow;
        this.audience = audience;
    }

    public void strike(UUID player, BadPacketKind kind, int count, int windowSeconds) {
        roster.active(player).ifPresent(record -> {
            audience.badPackets(record.name(), kind, count, windowSeconds);
            flow.process(record, IntegrityVerdict.strike());
        });
    }
}
