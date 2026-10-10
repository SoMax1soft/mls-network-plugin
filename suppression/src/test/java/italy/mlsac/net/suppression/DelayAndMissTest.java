// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Harness.STEVE;
import static italy.mlsac.net.suppression.fixture.Plans.lag;
import static italy.mlsac.net.suppression.fixture.Plans.ladder;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.damage.DamageOutcome;
import italy.mlsac.net.suppression.fixture.FakeDamageEvent;
import italy.mlsac.net.suppression.fixture.Harness;
import italy.mlsac.net.suppression.fixture.ScriptedRandom;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.LagLevels;
import italy.mlsac.net.suppression.lag.LagTable;
import italy.mlsac.net.suppression.lag.hit.HitVerdict;
import italy.mlsac.net.suppression.lag.hit.MissGate;
import italy.mlsac.net.suppression.lag.packet.DelayLag;
import italy.mlsac.net.suppression.lag.packet.HoldVerdict;
import italy.mlsac.net.suppression.lag.packet.PacketDisposition;
import italy.mlsac.net.suppression.lag.packet.PacketState;
import italy.mlsac.net.suppression.lag.runtime.LagRuntimes;
import italy.mlsac.net.suppression.server.LagExemptSuspects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DelayAndMissTest {

    private final ScriptedRandom random = new ScriptedRandom(0.5);
    private final AtomicReference<LagLevels> levels = new AtomicReference<>(LagLevels.none());
    private final LagRuntimes<String> runtimes = new LagRuntimes<>();
    private final DelayLag<String> delay = new DelayLag<>(id -> levels.get(), LagExemptSuspects.NONE, runtimes,
            LagTable.standard(), random);

    private void delayAt(LagLevel level) {
        levels.set(LagLevels.of(Map.of(LagKind.DELAY, level)));
    }

    private HoldVerdict<String> receive(String packet, long at) {
        return delay.onPacket(STEVE, packet, at, PacketState.LIVE);
    }

    @Test
    void g1JitteredHoldsKeepReceiveOrder() {
        delayAt(LagLevel.MEDIUM);
        random.queue(1.0, 0.0, 0.5);
        receive("a", 0);
        receive("b", 10);
        receive("c", 20);
        long due = 300L;
        assertEquals(List.of(due, due, due), runtimes.runtimeOf(STEVE).heldPackets().dueTimes());
        assertEquals(List.of(), delay.releaseDue(STEVE, due - 1));
        assertEquals(List.of("a", "b", "c"), delay.releaseDue(STEVE, due));
    }

    @Test
    void g2PacketsQueueBehindAfterDelayEndsUntilTheQueueDrains() {
        delayAt(LagLevel.LIGHT);
        random.queue(0.5);
        assertEquals(PacketDisposition.HOLD, receive("a", 0).disposition());
        levels.set(LagLevels.none());
        assertEquals(PacketDisposition.HOLD, receive("b", 60).disposition());
        assertEquals(List.of(100L, 100L), runtimes.runtimeOf(STEVE).heldPackets().dueTimes());
        assertEquals(List.of("a", "b"), delay.releaseDue(STEVE, 100));
        assertEquals(PacketDisposition.PASS, receive("c", 150).disposition());
    }

    @Test
    void g3FullQueueReleasesTheOldestImmediately() {
        delayAt(LagLevel.HEAVY);
        for (int i = 0; i < 40; i++) {
            assertEquals(List.of(), receive("p" + i, i).releaseNow());
        }
        HoldVerdict<String> overflow = receive("p40", 40);
        assertEquals(PacketDisposition.HOLD, overflow.disposition());
        assertEquals(List.of("p0"), overflow.releaseNow());
    }

    @Test
    void g4DeathDiscardsHeldPackets() {
        delayAt(LagLevel.HEAVY);
        receive("a", 0);
        receive("b", 1);
        receive("c", 2);
        assertEquals(3, runtimes.died(STEVE));
        assertEquals(List.of(), delay.releaseDue(STEVE, 10_000));
    }

    @Test
    void packetsCancelledByOthersAreNeverHeld() {
        delayAt(LagLevel.HEAVY);
        assertEquals(PacketDisposition.PASS,
                delay.onPacket(STEVE, "x", 0, PacketState.CANCELLED_BY_OTHERS).disposition());
    }

    @Test
    void g5LightMissRollsAgainstFiftyPercentWithCapThree() {
        MissGate gate = new MissGate();
        ScriptedRandom rolls = new ScriptedRandom(0.99).queue(0.10, 0.50, 0.20, 0.05, 0.30, 0.01);
        List<HitVerdict> verdicts = new ArrayList<>();
        for (int tick = 0; tick < 7; tick++) {
            verdicts.add(gate.judgeMainHit(LagTable.standard().miss(LagLevel.LIGHT), tick, rolls));
        }
        assertEquals(List.of(HitVerdict.MISSED, HitVerdict.COUNTS, HitVerdict.MISSED, HitVerdict.MISSED,
                HitVerdict.MISSED, HitVerdict.COUNTS, HitVerdict.MISSED), verdicts);
        assertEquals(6, rolls.draws());
    }

    @Test
    void g6MediumCapCountsTheNinthHitWithoutRolling() {
        MissGate gate = new MissGate();
        ScriptedRandom rolls = new ScriptedRandom(0.0);
        for (int tick = 0; tick < 8; tick++) {
            assertEquals(HitVerdict.MISSED, gate.judgeMainHit(LagTable.standard().miss(LagLevel.MEDIUM), tick, rolls));
        }
        assertEquals(8, rolls.draws());
        assertEquals(HitVerdict.COUNTS, gate.judgeMainHit(LagTable.standard().miss(LagLevel.MEDIUM), 8, rolls));
        assertEquals(8, rolls.draws());
        assertEquals(HitVerdict.MISSED, gate.judgeMainHit(LagTable.standard().miss(LagLevel.MEDIUM), 9, rolls));
    }

    @Test
    void heavyMissesEveryHitWithoutACap() {
        MissGate gate = new MissGate();
        ScriptedRandom rolls = new ScriptedRandom(0.999999);
        for (int tick = 0; tick < 200; tick++) {
            assertEquals(HitVerdict.MISSED, gate.judgeMainHit(LagTable.standard().miss(LagLevel.HEAVY), tick, rolls));
        }
        assertEquals(200, rolls.draws());
    }

    @Test
    void missedMainHitCancelsItsSweepsOnly() {
        Harness harness = new Harness(ladder(step(1, lag(LagLevel.HEAVY, 60, 0, LagKind.MISS))));
        harness.flag(1);
        harness.random.queue(0.0);
        FakeDamageEvent main = FakeDamageEvent.withFinal(6);
        assertEquals(DamageOutcome.MISSED, harness.assembly.damage().judgeDamage(main, "ENTITY_ATTACK", STEVE, null, 5));
        assertTrue(main.cancelled());
        FakeDamageEvent sweep = FakeDamageEvent.withFinal(1);
        assertEquals(DamageOutcome.MISSED,
                harness.assembly.damage().judgeDamage(sweep, "ENTITY_SWEEP_ATTACK", STEVE, null, 5));
        FakeDamageEvent laterSweep = FakeDamageEvent.withFinal(1);
        assertEquals(DamageOutcome.UNCHANGED,
                harness.assembly.damage().judgeDamage(laterSweep, "ENTITY_SWEEP_ATTACK", STEVE, null, 6));
    }
}
