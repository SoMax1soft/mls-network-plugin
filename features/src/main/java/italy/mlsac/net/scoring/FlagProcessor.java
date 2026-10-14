// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public final class FlagProcessor {

    private final ViolationLedger ledger;
    private final PlayerThread players;
    private final DetectionAudience audience;
    private final FlagPenalties penalties;

    public FlagProcessor(ViolationLedger ledger, PlayerThread players, DetectionAudience audience,
                         FlagPenalties penalties) {
        this.ledger = ledger;
        this.players = players;
        this.audience = audience;
        this.penalties = penalties;
    }

    public void process(SuspectRecord suspect, double probability, VerdictOrigin origin) {
        if (!players.online(suspect.id())) {
            return;
        }
        int offenceLevel = ledger.raise(suspect.id());
        FlagContext flag = new FlagContext(suspect.id(), suspect.name(), offenceLevel, probability, suspect.buffer(),
                origin);
        audience.flagRaised(flag);
        penalties.onFlag(flag);
    }
}
