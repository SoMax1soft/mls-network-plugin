// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.menu.SuspectRow;
import italy.mlsac.net.nametag.NametagPorts;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.SuspectRoster;
import italy.mlsac.net.scoring.SuspectSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SuspectRows {

    private final SuspectRoster roster;
    private final PlayerDirectory players;

    public SuspectRows(SuspectRoster roster, PlayerDirectory players) {
        this.roster = roster;
        this.players = players;
    }

    public List<SuspectRow> onlineSuspects() {
        return players.online().stream().map(view -> suspect(view.id())).flatMap(Optional::stream).toList();
    }

    public Optional<SuspectRow> suspect(UUID player) {
        return roster.active(player).filter(SuspectRecord::hasChecks).map(SuspectRecord::snapshot)
                .map(SuspectRows::row);
    }

    public long historyRevision(UUID player) {
        Optional<SuspectRecord> record = roster.active(player);
        return record.isPresent() ? record.get().historyRevision() : Long.MIN_VALUE;
    }

    public Optional<NametagPorts.HistoryView> history(UUID player) {
        return roster.active(player).map(SuspectRecord::modelHistory)
                .map(history -> new NametagPorts.HistoryView(history.revision(), history.results()));
    }

    private static SuspectRow row(SuspectSnapshot snapshot) {
        return new SuspectRow(snapshot.id(), snapshot.name(), snapshot.average(), snapshot.lastScores());
    }
}
