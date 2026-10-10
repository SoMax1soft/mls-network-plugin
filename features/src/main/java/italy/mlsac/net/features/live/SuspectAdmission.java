// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerStanding;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.player.StandingCache;
import italy.mlsac.net.region.RegionExemptions;
import italy.mlsac.net.scoring.CollectionFacts;
import italy.mlsac.net.scoring.CollectionVerdict;
import italy.mlsac.net.scoring.SuspectKind;
import italy.mlsac.net.scoring.SuspectRoster;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class SuspectAdmission {

    private static final String ROSTER_CLOCK = "suspects.roster";

    private final SuspectRoster roster;
    private final PlayerDirectory players;
    private final StandingCache standings;
    private final RegionExemptions regions;
    private final BooleanSupplier detectionEnabled;
    private final Clock clock;

    public SuspectAdmission(SuspectRoster roster, PlayerDirectory players, StandingCache standings,
                            RegionExemptions regions, BooleanSupplier detectionEnabled, Clock clock) {
        this.roster = roster;
        this.players = players;
        this.standings = standings;
        this.regions = regions;
        this.detectionEnabled = detectionEnabled;
        this.clock = clock;
    }

    public void everyOnline() {
        for (PlayerView view : players.online()) {
            consider(view);
        }
    }

    public void one(UUID player) {
        players.byId(player).ifPresent(this::consider);
    }

    public void departed(UUID player) {
        roster.depart(player, clock.wallMillis(ROSTER_CLOCK));
    }

    public void purge() {
        roster.purgeExpired(clock.wallMillis(ROSTER_CLOCK));
    }

    private void consider(PlayerView view) {
        if (!view.online() || roster.active(view.id()).isPresent()) {
            return;
        }
        PlayerStanding standing = standings.standing(view.id());
        SuspectKind kind = standing.bedrock() ? SuspectKind.BEDROCK : SuspectKind.JAVA;
        CollectionFacts facts = new CollectionFacts(detectionEnabled.getAsBoolean(), standing.bypass(), kind,
                regions.lastKnown(view.id()));
        if (CollectionVerdict.of(facts).collects()) {
            roster.admit(view.id(), view.name(), kind, clock.wallMillis(ROSTER_CLOCK));
        }
    }
}
