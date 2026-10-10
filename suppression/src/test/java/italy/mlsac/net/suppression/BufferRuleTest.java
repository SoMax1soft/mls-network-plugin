// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Harness.STEVE;
import static italy.mlsac.net.suppression.fixture.Plans.command;
import static italy.mlsac.net.suppression.fixture.Plans.dealt;
import static italy.mlsac.net.suppression.fixture.Plans.disabledRule;
import static italy.mlsac.net.suppression.fixture.Plans.rule;
import static italy.mlsac.net.suppression.fixture.Plans.rules;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static italy.mlsac.net.suppression.fixture.Plans.trick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.fixture.Harness;
import italy.mlsac.net.suppression.fixture.Plans;
import italy.mlsac.net.suppression.plan.BufferFiring;
import italy.mlsac.net.suppression.plan.BufferRules;
import italy.mlsac.net.suppression.plan.FlagLadder;
import italy.mlsac.net.suppression.plan.LadderRepeat;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.plan.TrickType;
import italy.mlsac.net.suppression.trigger.AnswerKind;
import italy.mlsac.net.suppression.trigger.BufferAnswer;
import italy.mlsac.net.suppression.trigger.BufferTrigger;
import italy.mlsac.net.suppression.trigger.SuspectOrigin;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BufferRuleTest {

    private static SuppressionPlan b1Plan(BufferFiring firing) {
        return rules(firing, 40, rule(25, 0, dealt(10, 1, 0)), rule(50, 10, dealt(20, 1, 0)));
    }

    private static List<Integer> fired(Optional<BufferTrigger> trigger) {
        return trigger.map(value -> value.firedRules().stream().map(rule -> rule.percent()).toList())
                .orElse(List.of());
    }

    private static void expect(Harness harness, long seconds, double buffer, List<Integer> fires, List<Integer> latched) {
        harness.at(seconds * 1000);
        assertEquals(fires, fired(harness.answer(buffer)), "fires at " + seconds + " s");
        assertEquals(latched, harness.engine().firedRulePercents(STEVE), "latches at " + seconds + " s");
    }

    @Test
    void b1LatchesCooldownsAndFlagReset() {
        Harness harness = new Harness(b1Plan(BufferFiring.ENABLED));
        expect(harness, 0, 5, List.of(), List.of());
        expect(harness, 1, 12, List.of(25), List.of(25));
        expect(harness, 2, 15, List.of(), List.of(25));
        expect(harness, 3, 8, List.of(), List.of());
        harness.at(4000);
        Optional<BufferTrigger> both = harness.answer(21);
        assertEquals(List.of(25, 50), fired(both));
        assertEquals(50, both.orElseThrow().highestPercent());
        expect(harness, 5, 19, List.of(), List.of(25));
        expect(harness, 6, 22, List.of(), List.of(25, 50));
        expect(harness, 7, 18, List.of(), List.of(25));
        expect(harness, 15, 25, List.of(50), List.of(25, 50));
        expect(harness, 16, 41, List.of(), List.of(25, 50));
        harness.flagReset(20);
        assertEquals(List.of(25, 50), harness.engine().firedRulePercents(STEVE));
        expect(harness, 17, 19.5, List.of(), List.of(25));
    }

    @Test
    void b2SwitchingFiringOnAtRuntimeDoesNotFireAlreadyCrossedRules() {
        Harness harness = new Harness(b1Plan(BufferFiring.DISABLED));
        double[] buffers = {5, 12, 15, 8, 21, 19, 22, 18};
        for (int second = 0; second < buffers.length; second++) {
            harness.at(second * 1000L);
            assertEquals(Optional.empty(), harness.answer(buffers[second]));
        }
        harness.at(8000);
        harness.applyPlan(b1Plan(BufferFiring.ENABLED));
        assertEquals(List.of(25), harness.engine().firedRulePercents(STEVE));
        harness.at(15_000);
        assertEquals(List.of(50), fired(harness.answer(25)));
    }

    @Test
    void b3FullBufferRuleFiresInTheSameAnswerAsTheFlag() {
        SuppressionPlan plan = new SuppressionPlan(
                new FlagLadder(LadderRepeat.REPEAT_LAST, List.of(step(1, command("kick {PLAYER}", 0)))),
                new BufferRules(BufferFiring.ENABLED, List.of(rule(100, 0, dealt(50, 30, 0)))),
                Plans.rules(BufferFiring.ENABLED, 40).capacity(), Plans.ladder().tricks(), Plans.ladder().consoleEcho());
        Harness harness = new Harness(plan);
        assertEquals(List.of(), fired(harness.answer(38)));
        assertEquals(List.of(100), fired(harness.answer(40.0)));
        harness.flag(1);
        assertEquals(50, harness.dealt());
        assertEquals(List.of("kick {PLAYER}"), harness.server.commandTexts());
    }

    @Test
    void b4DisabledRuleNeverFires() {
        Harness harness = new Harness(rules(BufferFiring.ENABLED, 40, disabledRule(25, dealt(10, 5, 0))));
        assertEquals(Optional.empty(), harness.answer(39));
        harness.answer(0);
        assertEquals(Optional.empty(), harness.answer(39));
        assertEquals(0, harness.dealt());
    }

    @Test
    void b5OnlyAlertAnswersNeverEvaluateRules() {
        Harness harness = new Harness(rules(BufferFiring.ENABLED, 40, rule(25, 0, dealt(10, 5, 0))));
        BufferAnswer onlyAlert = new BufferAnswer(harness.context(0, 0.99, 30), AnswerKind.ONLY_ALERT,
                SuspectOrigin.PLAYER);
        assertEquals(Optional.empty(), harness.engine().onAnswer(onlyAlert));
        assertEquals(List.of(), harness.engine().firedRulePercents(STEVE));
        assertEquals(List.of(25), fired(harness.answer(30)));
    }

    @Test
    void b6TricksRunOncePerTriggerAndOneStaffLineIsSent() {
        Harness harness = new Harness(rules(BufferFiring.ENABLED, 40,
                rule(30, 0, trick(TrickType.SHUFFLE_INVENTORY, 0), trick(TrickType.LAUNCH, 0)),
                rule(60, 0, trick(TrickType.SHUFFLE_INVENTORY, 0))));
        assertEquals(List.of(30, 60), fired(harness.answer(30)));
        assertEquals(List.of(0L), harness.timeline.timesOf("shuffle"));
        assertEquals(List.of(0L), harness.timeline.timesOf("launch"));
        assertEquals(1, harness.server.subscriberLines().size());
        assertTrue(harness.server.subscriberLines().get(0).endsWith("shuffle, launch"));
    }

    @Test
    void b7FlagResetToResetValueRearms() {
        Harness harness = new Harness(rules(BufferFiring.ENABLED, 40, rule(50, 0, dealt(10, 5, 0))));
        assertEquals(List.of(50), fired(harness.answer(41)));
        harness.flagReset(15);
        assertEquals(List.of(), harness.engine().firedRulePercents(STEVE));
        assertEquals(List.of(50), fired(harness.answer(21)));
    }

    @Test
    void c3AbsentSwitchMeansNoFiring() {
        Harness harness = new Harness(rules(BufferFiring.fromSwitch(null), 40, rule(25, 0, dealt(10, 5, 0)),
                rule(100, 0, dealt(90, 5, 0))));
        harness.answer(0);
        assertEquals(Optional.empty(), harness.answer(100));
        assertEquals(0, harness.dealt());
    }

    @Test
    void c9DefaultRulesFireTogetherAndStrongestWins() {
        SuppressionPlan plan = Plans.withRules(Plans.ladder(), BufferRules.defaults(BufferFiring.ENABLED));
        assertEquals(List.of(10.0, 20.0, 30.0),
                plan.bufferRules().rules().stream().map(plan::thresholdOf).toList());
        Harness harness = new Harness(plan);
        harness.answer(0);
        assertEquals(List.of(25, 50, 75), fired(harness.answer(31)));
        assertEquals(65, harness.dealt());
        assertEquals(75, harness.taken());
    }
}
