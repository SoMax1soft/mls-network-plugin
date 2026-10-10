// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.detection.DetectionStates;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.SuspectRoster;
import italy.mlsac.net.scoring.ViolationLedger;
import java.util.OptionalDouble;
import java.util.UUID;

public final class RosterStates implements DetectionStates {

    private final SuspectRoster roster;
    private final ViolationLedger ledger;

    public RosterStates(SuspectRoster roster, ViolationLedger ledger) {
        this.roster = roster;
        this.ledger = ledger;
    }

    @Override
    public OptionalDouble buffer(UUID player) {
        return roster.retained(player).map(SuspectRecord::buffer).map(OptionalDouble::of).orElse(OptionalDouble.empty());
    }

    @Override
    public int offenceLevel(UUID player) {
        return ledger.level(player);
    }
}
