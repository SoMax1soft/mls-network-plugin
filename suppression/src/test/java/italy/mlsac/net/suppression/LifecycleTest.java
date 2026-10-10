// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Harness.STEVE;
import static italy.mlsac.net.suppression.fixture.Plans.command;
import static italy.mlsac.net.suppression.fixture.Plans.dealt;
import static italy.mlsac.net.suppression.fixture.Plans.ladder;
import static italy.mlsac.net.suppression.fixture.Plans.rule;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static italy.mlsac.net.suppression.fixture.Plans.trick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.fixture.FakeServer.RanCommand;
import italy.mlsac.net.suppression.fixture.Harness;
import italy.mlsac.net.suppression.fixture.Plans;
import italy.mlsac.net.suppression.plan.BufferFiring;
import italy.mlsac.net.suppression.plan.BufferRules;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.plan.TrickType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LifecycleTest {

    private static final long FIFTEEN_MINUTES = 15L * 60L * 1000L;

    private static Harness sixtySecondEffect() {
        Harness harness = new Harness(ladder(step(1, dealt(50, 60, 0))));
        harness.flag(1);
        harness.at(10_000);
        harness.quit();
        return harness;
    }

    @Test
    void l1EffectTimerIsPausedWhileOfflineAndResumesOnRejoin() {
        Harness harness = sixtySecondEffect();
        harness.at(40_000);
        harness.join();
        harness.at(89_999);
        assertEquals(50, harness.dealt());
        harness.at(90_000);
        assertEquals(0, harness.dealt());
    }

    @Test
    void l1RejoinAfterTheOriginalEndStillRestoresTheRemainingTime() {
        Harness harness = sixtySecondEffect();
        harness.at(70_000);
        harness.join();
        harness.at(119_999);
        assertEquals(50, harness.dealt());
        harness.at(120_000);
        assertEquals(0, harness.dealt());
    }

    @Test
    void savedStateIsDeletedAfterFifteenMinutesOffline() {
        Harness harness = sixtySecondEffect();
        harness.at(10_000 + FIFTEEN_MINUTES + 1);
        harness.join();
        assertEquals(0, harness.dealt());
    }

    @Test
    void savedStateSurvivesExactlyFifteenMinutes() {
        Harness harness = sixtySecondEffect();
        harness.at(10_000 + FIFTEEN_MINUTES);
        harness.join();
        assertEquals(50, harness.dealt());
    }

    @Test
    void sweepDeletesExpiredOfflineState() {
        Harness harness = sixtySecondEffect();
        harness.tickAt(10_000 + FIFTEEN_MINUTES + 60_001);
        harness.at(10_000 + FIFTEEN_MINUTES + 60_002);
        harness.join();
        assertEquals(0, harness.dealt());
    }

    @Test
    void ruleLatchesAndCooldownsSurviveARejoin() {
        SuppressionPlan plan = Plans.rules(BufferFiring.ENABLED, 40, rule(25, 20, dealt(10, 5, 0)));
        Harness harness = new Harness(plan);
        assertTrue(harness.answer(12).isPresent());
        harness.at(5_000);
        harness.quit();
        harness.at(600_000);
        harness.join();
        assertEquals(List.of(25), harness.engine().firedRulePercents(STEVE));
        harness.answer(5);
        harness.at(610_000);
        assertEquals(Optional.empty(), harness.answer(12));
        harness.answer(5);
        harness.at(620_000);
        assertTrue(harness.answer(12).isPresent());
    }

    @Test
    void l2DelayedBanRunsWhileOfflineWithFlagContext() {
        Harness harness = new Harness(ladder(step(1, dealt(50, 30, 0), command("ban {PLAYER} VL {VL}", 1))));
        harness.flag(1, 0.93, 20.0);
        harness.at(10_000);
        harness.quit();
        harness.tickAt(29_950);
        assertEquals(List.of(), harness.server.commands());
        harness.tickAt(30_000);
        RanCommand ran = harness.server.commands().get(0);
        assertEquals("ban {PLAYER} VL {VL}", ran.text());
        assertEquals(harness.context(1, 0.93, 20.0), ran.context());
    }

    @Test
    void stageStartingWhileOfflineRecordsTimedEffectsAndDropsTricks() {
        Harness harness = new Harness(ladder(step(1, dealt(10, 10, 0), dealt(40, 20, 1),
                trick(TrickType.SHUFFLE_INVENTORY, 1))));
        harness.flag(1);
        harness.at(5_000);
        harness.quit();
        harness.tickAt(10_000);
        assertEquals(List.of(), harness.timeline.timesOf("shuffle"));
        harness.at(50_000);
        harness.join();
        harness.at(69_999);
        assertEquals(40, harness.dealt());
        harness.at(70_000);
        assertEquals(0, harness.dealt());
    }

    @Test
    void l3ShutdownDropsPendingCommandsWithOneWarning() {
        Harness harness = new Harness(ladder(step(1, dealt(10, 10, 0), command("say a", 1), command("say b", 1)),
                step(2, dealt(10, 10, 0), command("say c", 1))));
        harness.flag(1);
        harness.flag(2);
        harness.engine().release();
        harness.tickAt(20_000);
        assertEquals(List.of(), harness.server.commands());
        assertEquals(List.of("[Suppression] Dropped 3 pending suppression command(s) at shutdown"),
                harness.server.warnings());
    }

    @Test
    void l5ReloadKeepsEffectsAndChainsAndFiresNothing() {
        SuppressionPlan plan = Plans.withRules(ladder(step(1, dealt(50, 30, 0), command("kick {PLAYER}", 1))),
                new BufferRules(BufferFiring.ENABLED, List.of(rule(25, 60, dealt(10, 5, 0)))));
        Harness harness = new Harness(plan);
        harness.flag(1);
        assertTrue(harness.answer(12).isPresent());
        harness.answer(5);
        harness.answer(15);
        harness.at(5_000);
        harness.applyPlan(plan);
        assertEquals(50, harness.dealt());
        assertEquals(List.of(25), harness.engine().firedRulePercents(STEVE));
        harness.answer(5);
        harness.at(6_000);
        assertTrue(harness.answer(12).isPresent());
        harness.tickAt(30_000);
        assertEquals(List.of("kick {PLAYER}"), harness.server.commandTexts());
    }

    @Test
    void l6AFifthChainDropsTheOldestPendingStages() {
        Harness harness = new Harness(ladder(step(1, dealt(10, 60, 0), command("say kick", 1))));
        for (int flag = 0; flag < 5; flag++) {
            harness.at(flag * 200L);
            harness.flag(flag + 1);
        }
        assertEquals(4, harness.engine().pendingCommands());
        harness.tickAt(60_000);
        assertEquals(List.of(), harness.server.commands());
        harness.tickAt(60_200);
        assertEquals(1, harness.server.commands().size());
    }

    @Test
    void l7BypassSkipsEffectsButRunsCommands() {
        Harness harness = new Harness(ladder(step(1, dealt(50, 30, 0), command("kick {PLAYER}", 0))));
        harness.server.grantBypass(STEVE);
        harness.flag(1);
        assertEquals(0, harness.dealt());
        assertEquals(List.of("kick {PLAYER}"), harness.server.commandTexts());
        assertEquals(1, harness.server.subscriberLines().size());
        assertTrue(harness.server.subscriberLines().get(0).endsWith(": &fcmd kick"));
    }

    @Test
    void pausedSuspectKeepsTimersButFeelsNothingAndLosesTricks() {
        Harness harness = new Harness(ladder(step(1, dealt(50, 30, 0), trick(TrickType.LAUNCH, 0))));
        harness.server.pause(STEVE);
        harness.flag(1);
        assertEquals(0, harness.dealt());
        assertEquals(List.of(), harness.timeline.timesOf("launch"));
        harness.at(20_000);
        harness.server.unpause(STEVE);
        assertEquals(50, harness.dealt());
        harness.at(30_000);
        assertEquals(0, harness.dealt());
    }

    @Test
    void bypassPermissionIsCachedForFiveSeconds() {
        Harness harness = new Harness(ladder(step(1, dealt(50, 30, 0))));
        harness.flag(1);
        int before = harness.server.exemptionProbes();
        harness.dealt();
        harness.dealt();
        harness.at(4_999);
        harness.dealt();
        assertEquals(before, harness.server.exemptionProbes());
        harness.at(5_000);
        harness.dealt();
        assertEquals(before + 1, harness.server.exemptionProbes());
    }
}
