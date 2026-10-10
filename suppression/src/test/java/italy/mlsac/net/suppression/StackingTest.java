// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.effect.DamageDealtEffect;
import italy.mlsac.net.suppression.effect.DamageTakenEffect;
import italy.mlsac.net.suppression.effect.EffectBook;
import italy.mlsac.net.suppression.effect.EffectSnapshot;
import italy.mlsac.net.suppression.effect.LagEffect;
import italy.mlsac.net.suppression.effect.TimedEffect;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.LagLevels;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StackingTest {

    private static EffectSnapshot book(TimedEffect... effects) {
        EffectBook book = new EffectBook();
        for (TimedEffect effect : effects) {
            book.add(effect);
        }
        return book.snapshot(0);
    }

    @Test
    void s1StrongestActiveWinsAndWeakerLongerTakesOver() {
        EffectSnapshot snapshot = book(new DamageDealtEffect(30, 0, 30_000), new DamageDealtEffect(70, 10_000, 20_000));
        assertEquals(30, snapshot.dealtPercent(5_000));
        assertEquals(70, snapshot.dealtPercent(15_000));
        assertEquals(30, snapshot.dealtPercent(25_000));
        assertEquals(0, snapshot.dealtPercent(30_000));
    }

    @Test
    void s2InstancesNeverAddUp() {
        EffectSnapshot snapshot = book(new DamageDealtEffect(50, 0, 10_000), new DamageDealtEffect(50, 5_000, 15_000));
        assertEquals(50, snapshot.dealtPercent(12_000));
    }

    @Test
    void s3LagLevelIsComputedPerKind() {
        EffectSnapshot snapshot = book(
                new LagEffect(LagLevel.LIGHT, EnumSet.of(LagKind.DELAY, LagKind.MISS), 0, 60_000),
                new LagEffect(LagLevel.HEAVY, EnumSet.of(LagKind.MISS, LagKind.ROTATE), 10_000, 20_000));
        LagLevels during = snapshot.lagLevels(15_000);
        assertEquals(Optional.of(LagLevel.LIGHT), during.levelOf(LagKind.DELAY));
        assertEquals(Optional.of(LagLevel.HEAVY), during.levelOf(LagKind.MISS));
        assertEquals(Optional.of(LagLevel.HEAVY), during.levelOf(LagKind.ROTATE));
        LagLevels after = snapshot.lagLevels(25_000);
        assertEquals(Optional.of(LagLevel.LIGHT), after.levelOf(LagKind.DELAY));
        assertEquals(Optional.of(LagLevel.LIGHT), after.levelOf(LagKind.MISS));
        assertFalse(after.active(LagKind.ROTATE));
    }

    @Test
    void s4SeventeenthInstanceEvictsTheEarliestEnd() {
        EffectBook book = new EffectBook();
        book.add(new DamageDealtEffect(90, 0, 1_000));
        for (int i = 1; i < 16; i++) {
            book.add(new DamageDealtEffect(10, 0, 100_000 + i));
        }
        assertEquals(90, book.snapshot(0).dealtPercent(500));
        book.add(new DamageDealtEffect(20, 0, 200_000));
        EffectSnapshot snapshot = book.snapshot(0);
        assertEquals(16, snapshot.dealt().size());
        assertEquals(20, snapshot.dealtPercent(500));
        assertTrue(snapshot.dealt().stream().noneMatch(effect -> effect.percent() == 90));
    }

    @Test
    void s5DealtAndTakenAreIndependent() {
        EffectSnapshot snapshot = book(new DamageDealtEffect(40, 0, 10_000), new DamageTakenEffect(20, 0, 10_000));
        assertEquals(40, snapshot.dealtPercent(1_000));
        assertEquals(20, snapshot.takenPercent(1_000));
    }

    @Test
    void reapplyingRefreshesTheTimer() {
        EffectSnapshot snapshot = book(new DamageDealtEffect(40, 0, 10_000), new DamageDealtEffect(40, 8_000, 18_000));
        assertEquals(40, snapshot.dealtPercent(15_000));
    }
}
