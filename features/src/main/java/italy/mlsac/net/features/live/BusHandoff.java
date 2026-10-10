// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.detection.BufferChangeCause;
import italy.mlsac.net.detection.BufferChanged;
import italy.mlsac.net.detection.PlayerFlagged;
import italy.mlsac.net.handoff.SuppressionEngine;
import italy.mlsac.net.scoring.AnswerMoment;
import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.SuspectRoster;
import italy.mlsac.net.scoring.ViolationLedger;
import java.util.UUID;

public final class BusHandoff implements SuppressionEngine {

    private final EventBus bus;
    private final SuspectRoster roster;
    private final ViolationLedger ledger;

    public BusHandoff(EventBus bus, SuspectRoster roster, ViolationLedger ledger) {
        this.bus = bus;
        this.roster = roster;
        this.ledger = ledger;
    }

    @Override
    public void flagged(FlagContext flag) {
        bus.publish(new PlayerFlagged(flag.player(), flag.playerNick(), flag.offenceLevel(), flag.probability(),
                flag.buffer(), false));
    }

    @Override
    public void bufferRaised(AnswerMoment moment) {
        SuspectRecord suspect = moment.suspect();
        bus.publish(new BufferChanged(suspect.id(), suspect.name(), moment.buffer(), BufferChangeCause.VERDICT,
                moment.verdict().probability(), ledger.level(suspect.id())));
    }

    @Override
    public void bufferLowered(UUID player, double buffer) {
        String nick = roster.retained(player).map(SuspectRecord::name).orElse(player.toString());
        bus.publish(new BufferChanged(player, nick, buffer, BufferChangeCause.FLAG_RESET, 0.0, ledger.level(player)));
    }
}
