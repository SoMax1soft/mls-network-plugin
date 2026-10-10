// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Harness.STEVE;
import static italy.mlsac.net.suppression.fixture.Plans.dealt;
import static italy.mlsac.net.suppression.fixture.Plans.lag;
import static italy.mlsac.net.suppression.fixture.Plans.ladder;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.damage.DamageOutcome;
import italy.mlsac.net.suppression.fixture.FakeDamageEvent;
import italy.mlsac.net.suppression.fixture.Harness;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import org.junit.jupiter.api.Test;

class RecordingLagSkipTest {

    private final Harness harness = new Harness(ladder(step(1,
            lag(LagLevel.HEAVY, 60, 0, LagKind.DELAY, LagKind.MISS, LagKind.SETBACK, LagKind.GROUND, LagKind.ROTATE),
            dealt(20, 60, 0))));

    @Test
    void recordedSuspectHasNoLagKindButTheStageStillFiresAndDamageIsScaled() {
        harness.server.startRecording(STEVE);
        harness.flag(1);
        assertEquals(1, harness.server.subscriberLines().size());
        assertTrue(harness.readout().lagLevels(STEVE).isEmpty());
        assertEquals(20, harness.dealt());
        harness.random.queue(0.0);
        FakeDamageEvent hit = FakeDamageEvent.withFinal(10);
        assertEquals(DamageOutcome.SCALED,
                harness.assembly.damage().judgeDamage(hit, "ENTITY_ATTACK", STEVE, null, 5));
    }

    @Test
    void lagComesBackWhenTheRecordingStopsWhileItIsStillActive() {
        harness.server.startRecording(STEVE);
        harness.flag(1);
        harness.at(10_000);
        harness.server.stopRecording(STEVE);
        for (LagKind kind : LagKind.values()) {
            assertTrue(harness.readout().lagLevels(STEVE).active(kind), kind.name());
        }
        harness.random.queue(0.0);
        FakeDamageEvent hit = FakeDamageEvent.withFinal(10);
        assertEquals(DamageOutcome.MISSED,
                harness.assembly.damage().judgeDamage(hit, "ENTITY_ATTACK", STEVE, null, 5));
    }

    @Test
    void recordingStartedMidLagSkipsTheMissFromThen() {
        harness.flag(1);
        harness.random.queue(0.0);
        assertEquals(DamageOutcome.MISSED, harness.assembly.damage()
                .judgeDamage(FakeDamageEvent.withFinal(10), "ENTITY_ATTACK", STEVE, null, 5));
        harness.server.startRecording(STEVE);
        harness.random.queue(0.0);
        assertEquals(DamageOutcome.SCALED, harness.assembly.damage()
                .judgeDamage(FakeDamageEvent.withFinal(10), "ENTITY_ATTACK", STEVE, null, 6));
    }
}
