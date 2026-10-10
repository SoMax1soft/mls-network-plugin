// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.handoff.AuthorityRoute;
import italy.mlsac.net.handoff.PenaltyAuthority;
import italy.mlsac.net.handoff.SuppressionEngine;
import italy.mlsac.net.scoring.AnswerMoment;
import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.scoring.ScreenedVerdict;
import italy.mlsac.net.scoring.SuspectKind;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.VerdictWeight;
import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.RecordingLog;
import italy.mlsac.net.testkit.TickQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LegacyResponsesTest implements TrickHands {

    private final RecordingLog log = new RecordingLog();
    private final TickQueue ticks = new TickQueue();
    private final DamageReductions reductions = new DamageReductions();
    private final TrickCooldowns cooldowns = new TrickCooldowns();
    private final List<String> tricks = new ArrayList<>();
    private final List<String> announcements = new ArrayList<>();
    private final SuspectRecord suspect = new SuspectRecord(UUID.randomUUID(), "Cheater", SuspectKind.JAVA);
    private long now = 0;
    private boolean holdsSword;
    private ResponseRules rules = ResponseRules.pluginDefaults();
    private final LegacyAlertResponses responses = new LegacyAlertResponses(() -> rules, ticks, reductions, cooldowns,
            this, announcements::add, BundledResources.colors(), () -> now, log);

    private void answer(double buffer) {
        suspect.resetBuffer(buffer);
        responses.answerAccepted(new AnswerMoment(suspect,
                new ScreenedVerdict("ultra", 0.97, VerdictWeight.COUNTED, "Ultra"), buffer));
        ticks.tick();
    }

    @Test
    void damageStageHalvesDamageForItsDuration() {
        ticks.online.add(suspect.id());
        rules = new ResponseRules(true, true, List.of(new ReductionStage(10, 50, 8, true)), false, List.of());
        answer(9);
        assertEquals(1.0, reductions.multiplier(suspect.id(), now));
        answer(11);
        assertEquals(0.5, reductions.multiplier(suspect.id(), 7_999));
        assertEquals(1.0, reductions.multiplier(suspect.id(), 8_000));
    }

    @Test
    void strongerOrLongerReductionReplaces() {
        UUID id = UUID.randomUUID();
        reductions.offer(id, 35, 12_000, 0);
        reductions.offer(id, 5, 8_000, 0);
        assertEquals(0.65, reductions.multiplier(id, 1), 1e-9);
        reductions.offer(id, 5, 20_000, 0);
        assertEquals(0.95, reductions.multiplier(id, 1), 1e-9);
        reductions.offer(id, 0.005, 30_000, 0);
        assertEquals(1.0, reductions.multiplier(id, 1));
    }

    @Test
    void offlineSuspectGetsNothing() {
        rules = new ResponseRules(true, true, List.of(new ReductionStage(10, 50, 8, true)), false, List.of());
        answer(20);
        assertEquals(1.0, reductions.multiplier(suspect.id(), 0));
    }

    @Test
    void dropWeaponWithAxeDoesNothingAndStartsNoCooldown() {
        ticks.online.add(suspect.id());
        rules = new ResponseRules(true, false, List.of(), true, List.of(
                new TrollTrick("drop_weapon", 20, true, 30, true, 1.9, 0.55, "&c{PLAYER} dropped at {BUFFER}")));
        answer(25);
        assertTrue(announcements.isEmpty());
        assertTrue(cooldowns.ready(suspect.id(), "drop_weapon", 0));
        answer(5);
        holdsSword = true;
        answer(25);
        assertEquals(List.of("§cCheater dropped at 25.0"), announcements);
        assertEquals(false, cooldowns.ready(suspect.id(), "drop_weapon", 29_999));
    }

    @Test
    void cooldownConsumesTheCrossing() {
        ticks.online.add(suspect.id());
        rules = new ResponseRules(true, false, List.of(), true, List.of(
                new TrollTrick("launch", 20, true, 30, false, 1.2, 0.6, "")));
        answer(25);
        answer(5);
        now = 10_000;
        answer(25);
        now = 40_000;
        answer(26);
        assertEquals(List.of("launch"), tricks);
        answer(5);
        answer(25);
        assertEquals(List.of("launch", "launch"), tricks);
    }

    @Test
    void unknownTrickWarns() {
        ticks.online.add(suspect.id());
        rules = new ResponseRules(true, false, List.of(), true, List.of(
                new TrollTrick("dance", 20, true, 0, false, 1, 1, "")));
        answer(25);
        assertEquals(1, log.warnings.size());
        assertEquals("troll-dance-20.0", rules.tricks().get(0).latchName());
        assertEquals("dr-10.0", new ReductionStage(10, 5, 8, true).latchName());
    }

    @Test
    void baseDamageCorrection() {
        assertEquals(5.0, BaseDamageCorrection.correctedBase(10, 8, 4), 1e-9);
        assertEquals(12.0, BaseDamageCorrection.correctedBase(10, 0, 2), 1e-9);
        assertTrue(BaseDamageCorrection.needsCorrection(4.5, 4));
    }

    @Test
    void authorityRouteSwitchesBetweenLadderAndSuppression() {
        List<String> calls = new ArrayList<>();
        AtomicReference<PenaltyAuthority> authority = new AtomicReference<>(PenaltyAuthority.of(false));
        SuppressionEngine engine = new SuppressionEngine() {
            @Override
            public void flagged(FlagContext flag) {
                calls.add("engine flag " + flag.offenceLevel());
            }

            @Override
            public void bufferRaised(AnswerMoment moment) {
                calls.add("engine buffer " + moment.buffer());
            }

            @Override
            public void bufferLowered(UUID player, double buffer) {
                calls.add("engine lowered " + buffer);
            }
        };
        AuthorityRoute route = new AuthorityRoute(authority::get, flag -> calls.add("ladder " + flag.offenceLevel()),
                responses, engine);
        FlagContext flag = new FlagContext(suspect.id(), "Cheater", 2, 0.9, 20);
        route.onFlag(flag);
        assertEquals(responses, route.get());
        authority.set(PenaltyAuthority.SUPPRESSION_ENGINE);
        route.onFlag(flag);
        AnswerMoment moment = new AnswerMoment(suspect, new ScreenedVerdict("ultra", 1, VerdictWeight.COUNTED, "U"), 30);
        route.get().bufferMoved(moment);
        route.get().answerAccepted(moment);
        route.get().bufferReset(suspect, 20);
        assertEquals(List.of("ladder 2", "engine flag 2", "engine buffer 30.0", "engine lowered 20.0"), calls);
        assertEquals(0, ticks.pendingCount());
    }

    @Override
    public boolean scrambleInventory(UUID player) {
        tricks.add("shuffle");
        return true;
    }

    @Override
    public boolean flingHeldItem(UUID player, boolean bladeRequired, double horizontal, double vertical) {
        if (bladeRequired && !holdsSword) {
            return false;
        }
        tricks.add("drop");
        return true;
    }

    @Override
    public boolean launch(UUID player, double horizontal, double vertical) {
        tricks.add("launch");
        return true;
    }
}
