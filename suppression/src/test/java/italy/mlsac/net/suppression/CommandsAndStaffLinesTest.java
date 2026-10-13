// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Harness.STEVE;
import static italy.mlsac.net.suppression.fixture.Plans.animated;
import static italy.mlsac.net.suppression.fixture.Plans.command;
import static italy.mlsac.net.suppression.fixture.Plans.dealt;
import static italy.mlsac.net.suppression.fixture.Plans.lag;
import static italy.mlsac.net.suppression.fixture.Plans.ladder;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static italy.mlsac.net.suppression.fixture.Plans.trick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.fixture.BuiltInTexts;
import italy.mlsac.net.suppression.fixture.Harness;
import italy.mlsac.net.suppression.fixture.Plans;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.plan.BufferFiring;
import italy.mlsac.net.suppression.plan.BufferRules;
import italy.mlsac.net.suppression.plan.ConsoleEcho;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.plan.TrickType;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import italy.mlsac.net.suppression.trigger.SuspectOrigin;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandsAndStaffLinesTest {

    @Test
    void p3LadderEquivalentToCompiledActionsProducesTheSameCommands() {
        Harness harness = new Harness(ladder(step(1, command("kick {PLAYER} MLSAC Detection (VL:{VL})", 0)),
                step(2, animated("ban {PLAYER} Cheating (MLSAC VL:{VL})", 0))));
        harness.flag(1);
        harness.at(10_000);
        harness.flag(2);
        harness.at(20_000);
        harness.flag(3);
        assertEquals(List.of("kick {PLAYER} MLSAC Detection (VL:{VL})",
                "{ANIMATION} ban {PLAYER} Cheating (MLSAC VL:{VL})",
                "{ANIMATION} ban {PLAYER} Cheating (MLSAC VL:{VL})"), harness.server.commandTexts());
        assertEquals(List.of(1, 2, 3), harness.server.commands().stream().map(ran -> ran.context().vl()).toList());
    }

    @Test
    void k2DelayedKickRunsExactlyOnce() {
        Harness harness = new Harness(ladder(step(1, dealt(20, 30, 0), command("kick {PLAYER}", 1))));
        harness.flag(1);
        for (long time = 0; time <= 90_000; time += 50) {
            harness.tickAt(time);
        }
        assertEquals(List.of(30_000L), harness.timeline.timesOf("cmd"));
    }

    @Test
    void k3PunishmentCooldownSkipsOnlyTheCommandsOfTheStage() {
        Harness harness = new Harness(ladder(step(1, dealt(20, 30, 0), command("kick {PLAYER}", 0))));
        harness.flag(1);
        harness.at(3_000);
        harness.flag(2);
        assertEquals(List.of("kick {PLAYER}"), harness.server.commandTexts());
        assertEquals(2, harness.server.subscriberLines().size());
        assertTrue(harness.server.subscriberLines().get(1).endsWith("-20% dmg 30s, cmd skipped (cooldown)"));
        harness.at(8_000);
        harness.flag(3);
        assertEquals(2, harness.server.commands().size());
    }

    @Test
    void nonPunishingStagesNeverHitTheCooldown() {
        Harness harness = new Harness(ladder(step(1, command("say hi", 0))));
        harness.flag(1);
        harness.flag(2);
        assertEquals(2, harness.server.commands().size());
    }

    @Test
    void k7BenchFlagsStartNothing() {
        Harness harness = new Harness(ladder(step(1, dealt(20, 30, 0), command("kick {PLAYER}", 0))));
        harness.engine().onFlag(new FlagEvent(harness.context(1, 0.9, 20), SuspectOrigin.BENCH));
        assertTrue(harness.server.commands().isEmpty());
        assertEquals(0, harness.dealt());
        assertEquals(0, harness.engine().pendingCommands());
    }

    @Test
    void v1FlagStageLineListsTokensAndStagePosition() {
        Harness harness = new Harness(ladder(step(1, dealt(50, 30, 0),
                lag(LagLevel.MEDIUM, 20, 0, LagKind.DELAY, LagKind.MISS), command("kick {PLAYER}", 1))));
        harness.server.override("suppression-alert-flag", "{PLAYER}|{VL}|{STAGE}/{STAGES}|{ACTIONS}");
        harness.flag(1);
        assertEquals(List.of("Steve|1|1/2|-50% dmg 30s, lag medium [delay,miss] 20s"),
                harness.server.subscriberLines());
    }

    @Test
    void v2BufferTriggerLineListsEveryFiredRule() {
        SuppressionPlan plan = Plans.withRules(ladder(), BufferRules.defaults(BufferFiring.ENABLED));
        Harness harness = new Harness(plan);
        harness.server.override("suppression-alert-buffer", "{PERCENT}|{BUFFER}|{ACTIONS}");
        harness.answer(0);
        harness.answer(21.0);
        assertEquals(List.of("50|21.0|-25% dmg 30s, +10% taken 30s, -35% dmg 35s, +15% taken 30s"),
                harness.server.subscriberLines());
    }

    @Test
    void v4ConsoleEchoFollowsAlertsConsole() {
        Harness silent = new Harness(ladder(step(1, command("say hi", 0))));
        silent.flag(1);
        assertTrue(silent.server.consoleLines().isEmpty());
        Harness echoing = new Harness(Plans.withEcho(ladder(step(1, command("say hi", 0))),
                ConsoleEcho.ECHO_STAFF_LINES));
        echoing.flag(1);
        assertEquals(echoing.server.subscriberLines(), echoing.server.consoleLines());
    }

    @Test
    void v5NothingAppliedMeansNoLine() {
        Harness harness = new Harness(ladder(step(1, dealt(50, 30, 0))));
        harness.server.grantBypass(STEVE);
        harness.flag(1);
        assertTrue(harness.server.subscriberLines().isEmpty());
    }

    @Test
    void v6EmptyOverrideDisablesTheLine() {
        Harness harness = new Harness(ladder(step(1, dealt(50, 30, 0))));
        harness.server.override("suppression-alert-flag", "");
        harness.flag(1);
        assertTrue(harness.server.subscriberLines().isEmpty());
        assertEquals(50, harness.dealt());
    }

    @Test
    void v7StaffLinesTakeTheConfiguredPrefix() {
        Harness harness = new Harness(ladder(step(1, dealt(50, 30, 0))));
        harness.server.prefix("&bGrim &8» &r");
        harness.server.override("suppression-alert-flag", "{PLAYER}|{STAGE}/{STAGES}");
        harness.flag(1);
        assertEquals(List.of("&bGrim &8» &rSteve|1/1"), harness.server.subscriberLines());
        Harness stock = new Harness(ladder(step(1, dealt(50, 30, 0))));
        stock.server.prefix(BuiltInTexts.PREFIX);
        stock.flag(1);
        assertTrue(stock.server.subscriberLines().get(0).startsWith(BuiltInTexts.PREFIX + "&fSteve &7Suppression"));
    }

    @Test
    void oneStageAnnouncesARepeatedTrickOnceAndItsStageLineOnce() {
        Harness harness = new Harness(ladder(step(1, trick(TrickType.LAUNCH, 0), trick(TrickType.LAUNCH, 0))));
        harness.flag(1);
        assertEquals(2, harness.timeline.timesOf("launch").size());
        assertEquals(1, harness.server.staffLines().size());
        assertEquals(1, harness.server.subscriberLines().size());
        assertNotEquals(harness.server.staffLines().get(0), harness.server.subscriberLines().get(0));
    }
}
