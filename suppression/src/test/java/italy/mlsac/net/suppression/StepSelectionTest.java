// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Plans.command;
import static italy.mlsac.net.suppression.fixture.Plans.dealt;
import static italy.mlsac.net.suppression.fixture.Plans.ladder;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.fixture.Harness;
import italy.mlsac.net.suppression.plan.FlagLadder;
import italy.mlsac.net.suppression.plan.LadderRepeat;
import italy.mlsac.net.suppression.plan.LadderStep;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StepSelectionTest {

    private static FlagLadder ladderOf(LadderRepeat repeat, int... flags) {
        LadderStep[] steps = new LadderStep[flags.length];
        for (int i = 0; i < flags.length; i++) {
            steps[i] = step(flags[i], command("say " + flags[i], 0));
        }
        return new FlagLadder(repeat, List.of(steps));
    }

    private static Optional<Integer> chosen(FlagLadder ladder, int flag) {
        return ladder.stepFor(flag).map(LadderStep::flag);
    }

    @Test
    void e8RepeatLastRepeatsTheHighestStep() {
        FlagLadder ladder = ladderOf(LadderRepeat.REPEAT_LAST, 1, 2, 3);
        assertEquals(Optional.of(1), chosen(ladder, 1));
        assertEquals(Optional.of(2), chosen(ladder, 2));
        assertEquals(Optional.of(3), chosen(ladder, 3));
        assertEquals(Optional.of(3), chosen(ladder, 4));
        assertEquals(Optional.of(3), chosen(ladder, 50));
    }

    @Test
    void e9WithoutRepeatNothingRunsAboveTheLastStep() {
        assertEquals(Optional.empty(), chosen(ladderOf(LadderRepeat.STOP_AFTER_LAST, 1, 2, 3), 4));
    }

    @Test
    void e10GapsRunNothing() {
        FlagLadder ladder = ladderOf(LadderRepeat.REPEAT_LAST, 1, 3);
        assertEquals(Optional.empty(), chosen(ladder, 2));
        assertEquals(Optional.of(3), chosen(ladder, 3));
        assertEquals(Optional.of(3), chosen(ladder, 5));
    }

    @Test
    void e11EmptyLadderNeverRunsAnything() {
        Harness harness = new Harness(ladder());
        for (int vl = 1; vl <= 5; vl++) {
            harness.flag(vl);
        }
        assertTrue(harness.server.commandTexts().isEmpty());
        assertEquals(0, harness.dealt());
        assertTrue(harness.server.subscriberLines().isEmpty());
    }

    @Test
    void e12SingleLaterStep() {
        FlagLadder ladder = ladderOf(LadderRepeat.REPEAT_LAST, 2);
        assertEquals(Optional.empty(), chosen(ladder, 1));
        assertEquals(Optional.of(2), chosen(ladder, 2));
        assertEquals(Optional.of(2), chosen(ladder, 3));
    }

    @Test
    void k6FlagOnAnEffectOnlyStepDoesNotRerunTheEarlierKick() {
        Harness harness = new Harness(ladder(step(1, command("kick {PLAYER}", 0)), step(2, dealt(30, 10, 0))));
        harness.flag(1);
        harness.at(10_000);
        harness.flag(2);
        assertEquals(List.of("kick {PLAYER}"), harness.server.commandTexts());
        assertEquals(30, harness.dealt());
    }
}
