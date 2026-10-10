// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Plans.command;
import static italy.mlsac.net.suppression.fixture.Plans.dealt;
import static italy.mlsac.net.suppression.fixture.Plans.ladder;
import static italy.mlsac.net.suppression.fixture.Plans.rule;
import static italy.mlsac.net.suppression.fixture.Plans.rules;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.fixture.Harness;
import italy.mlsac.net.suppression.fixture.RecordingLedger;
import italy.mlsac.net.suppression.plan.BufferFiring;
import java.util.List;
import org.junit.jupiter.api.Test;

class SuppressionLedgerTest {

    @Test
    void aFiredBufferRuleIsRecordedWithItsActions() {
        Harness harness = new Harness(rules(BufferFiring.ENABLED, 40, rule(25, 0, dealt(10, 1, 0))));
        harness.at(1_000);
        harness.answer(12);
        assertEquals(List.of(new RecordingLedger.Entry("buffer", 25, Harness.STEVE_NAME, 12.0,
                List.of("-10% dmg 1s"))), harness.ledger.entries());
    }

    @Test
    void aStageRecordsItsInGameActionsButNotItsCommands() {
        Harness harness = new Harness(ladder(step(1, dealt(20, 30, 0), command("kick {PLAYER}", 0))));
        harness.flag(1);
        assertEquals(List.of("kick {PLAYER}"), harness.server.commandTexts());
        assertEquals(List.of(new RecordingLedger.Entry("stage", 1, Harness.STEVE_NAME, 20.0,
                List.of("-20% dmg 30s"))), harness.ledger.entries());
    }

    @Test
    void aStageWithOnlyCommandsIsNotSuppression() {
        Harness harness = new Harness(ladder(step(1, command("kick {PLAYER}", 0))));
        harness.flag(1);
        assertEquals(1, harness.server.commands().size());
        assertTrue(harness.ledger.entries().isEmpty());
    }

    @Test
    void aBufferBelowEveryRuleRecordsNothing() {
        Harness harness = new Harness(rules(BufferFiring.ENABLED, 40, rule(25, 0, dealt(10, 1, 0))));
        harness.at(1_000);
        harness.answer(5);
        assertTrue(harness.ledger.entries().isEmpty());
    }
}
