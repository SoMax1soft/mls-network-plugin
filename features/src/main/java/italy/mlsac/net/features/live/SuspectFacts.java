// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.packet.ClientVersions;
import italy.mlsac.net.profile.ProfileFacts;
import italy.mlsac.net.punishment.PunishSubcommand;
import italy.mlsac.net.detection.AimProfiles;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.SuspectRoster;
import italy.mlsac.net.scoring.ViolationLedger;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class SuspectFacts implements ProfileFacts, PunishSubcommand.PunishEvidence {

    private final SuspectRoster roster;
    private final ViolationLedger ledger;
    private final AimProfiles aim;
    private final ClientVersions clients;

    public SuspectFacts(SuspectRoster roster, ViolationLedger ledger, AimProfiles aim, ClientVersions clients) {
        this.roster = roster;
        this.ledger = ledger;
        this.aim = aim;
        this.clients = clients;
    }

    @Override
    public OptionalInt sensitivityPercent(UUID player) {
        return aim.sensitivityPercent(player);
    }

    @Override
    public Optional<String> clientVersion(UUID player) {
        return clients.versionName(player);
    }

    @Override
    public int highProbabilityCount(UUID player) {
        return roster.retained(player).map(SuspectRecord::highProbabilityCount).orElse(0);
    }

    @Override
    public int offenceLevel(UUID player) {
        return ledger.level(player);
    }

    @Override
    public double buffer(UUID player) {
        return roster.retained(player).map(SuspectRecord::buffer).orElse(0.0);
    }
}
