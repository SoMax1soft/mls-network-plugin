// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Harness.STEVE;
import static italy.mlsac.net.suppression.fixture.Plans.dealt;
import static italy.mlsac.net.suppression.fixture.Plans.ladder;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static italy.mlsac.net.suppression.fixture.Plans.taken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import italy.mlsac.net.suppression.damage.DamageOutcome;
import italy.mlsac.net.suppression.damage.DamageSuppression;
import italy.mlsac.net.suppression.fixture.FakeDamageEvent;
import italy.mlsac.net.suppression.fixture.Harness;
import italy.mlsac.net.suppression.plan.LadderStep;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import italy.mlsac.net.suppression.trigger.SuspectOrigin;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DamageTest {

    private static final UUID ALEX = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-000000018ea0");

    private static DamageSuppression hook(int attackerDealt, int victimTaken) {
        List<LadderStep> steps = new ArrayList<>();
        if (attackerDealt > 0) {
            steps.add(step(1, dealt(attackerDealt, 60, 0)));
        }
        if (victimTaken > 0) {
            steps.add(step(2, taken(victimTaken, 60, 0)));
        }
        Harness harness = new Harness(ladder(steps.toArray(new LadderStep[0])));
        harness.engine().onFlag(new FlagEvent(new TriggerContext(STEVE, "Steve", 1, 0.9, 20), SuspectOrigin.PLAYER));
        harness.engine().onFlag(new FlagEvent(new TriggerContext(ALEX, "Alex", 2, 0.9, 20), SuspectOrigin.PLAYER));
        return harness.assembly.damage();
    }

    private static double hit(int dealtPercent, int takenPercent, double finalDamage) {
        FakeDamageEvent event = FakeDamageEvent.withFinal(finalDamage);
        hook(dealtPercent, takenPercent).judgeDamage(event, "ENTITY_ATTACK", STEVE, ALEX, 1);
        assertFalse(event.cancelled());
        return event.finalDamage();
    }

    @Test
    void d1DealtReducesFinalDamage() {
        assertEquals(6.0, hit(25, 0, 8.0), 0.01);
    }

    @Test
    void d2FullReductionGivesZeroWithoutCancelling() {
        assertEquals(0.0, hit(100, 0, 8.0), 0.01);
    }

    @Test
    void d3TakenMultipliesFinalDamage() {
        assertEquals(20.0, hit(0, 300, 5.0), 0.01);
    }

    @Test
    void d4BothEffectsCombineInOneMultiplier() {
        assertEquals(10.5, hit(30, 50, 10.0), 0.01);
    }

    @Test
    void d5ZeroDamageIsLeftAlone() {
        FakeDamageEvent event = FakeDamageEvent.withFinal(0.0);
        assertEquals(DamageOutcome.UNCHANGED, hook(50, 50).judgeDamage(event, "ENTITY_ATTACK", STEVE, ALEX, 1));
        assertEquals(0, event.writes());
    }

    @Test
    void d6NoEffectsNoWork() {
        DamageSuppression hook = hook(0, 0);
        FakeDamageEvent event = FakeDamageEvent.withFinal(7.0);
        assertFalse(hook.concerns(STEVE, ALEX));
        assertEquals(DamageOutcome.UNCHANGED, hook.judgeDamage(event, "ENTITY_ATTACK", STEVE, ALEX, 1));
        assertEquals(0, event.writes());
        assertEquals(7.0, event.finalDamage(), 1e-9);
    }

    @Test
    void baseDamageFallbackWhenComponentsAreMissing() {
        FakeDamageEvent event = FakeDamageEvent.withoutComponents(8.0);
        assertEquals(DamageOutcome.SCALED, hook(25, 0).judgeDamage(event, "ENTITY_ATTACK", STEVE, ALEX, 1));
        assertEquals(6.0, event.finalDamage(), 0.01);
    }

    @Test
    void takenIgnoresExcludedCausesAndCountsOthers() {
        DamageSuppression hook = hook(0, 300);
        for (String cause : List.of("FALL", "VOID", "DROWNING", "SUFFOCATION", "STARVATION", "KILL")) {
            FakeDamageEvent event = FakeDamageEvent.withFinal(2.0);
            hook.judgeDamage(event, cause, null, ALEX, 1);
            assertEquals(2.0, event.finalDamage(), 1e-9, cause);
        }
        for (String cause : List.of("LAVA", "ENTITY_EXPLOSION", "ENTITY_ATTACK", "THORNS")) {
            FakeDamageEvent event = FakeDamageEvent.withFinal(2.0);
            hook.judgeDamage(event, cause, null, ALEX, 1);
            assertEquals(8.0, event.finalDamage(), 0.01, cause);
        }
    }

    @Test
    void thornsFromTheSuspectsArmourIsNotReduced() {
        FakeDamageEvent event = FakeDamageEvent.withFinal(3.0);
        hook(50, 0).judgeDamage(event, "THORNS", STEVE, null, 1);
        assertEquals(3.0, event.finalDamage(), 1e-9);
        FakeDamageEvent arrow = FakeDamageEvent.withFinal(3.0);
        hook(50, 0).judgeDamage(arrow, "PROJECTILE", STEVE, null, 1);
        assertEquals(1.5, arrow.finalDamage(), 0.01);
    }
}
