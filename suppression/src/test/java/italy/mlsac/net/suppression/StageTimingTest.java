// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Plans.command;
import static italy.mlsac.net.suppression.fixture.Plans.dealt;
import static italy.mlsac.net.suppression.fixture.Plans.lag;
import static italy.mlsac.net.suppression.fixture.Plans.ladder;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static italy.mlsac.net.suppression.fixture.Plans.taken;
import static italy.mlsac.net.suppression.fixture.Plans.trick;
import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.suppression.chain.LadderChain;
import italy.mlsac.net.suppression.chain.ChainStage;
import italy.mlsac.net.suppression.fixture.Harness;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.plan.LadderStep;
import italy.mlsac.net.suppression.plan.TrickType;
import java.util.List;
import org.junit.jupiter.api.Test;

class StageTimingTest {

    private static void serverTicks(Harness harness, long from, long to) {
        for (long time = from; time <= to; time += 50) {
            harness.tickAt(time);
        }
    }

    private static List<Long> starts(LadderStep step, long beganAt) {
        return LadderChain.start(harness().context(1, 0.9, 20), step, beganAt).stages().stream()
                .map(ChainStage::startAt).toList();
    }

    private static Harness harness() {
        return new Harness(ladder());
    }

    @Test
    void e1TimedStageDelaysTheKickByItsLongestEffect() {
        LadderStep step = step(1, dealt(50, 30, 0), lag(LagLevel.MEDIUM, 20, 0, LagKind.DELAY, LagKind.MISS),
                command("kick {PLAYER}", 1));
        assertEquals(List.of(0L, 30_000L), starts(step, 0));
        Harness harness = new Harness(ladder(step));
        harness.flag(1);
        assertEquals(List.of(0L, 0L), harness.timeline.timesOf("effects"));
        serverTicks(harness, 50, 29_950);
        assertEquals(List.of(), harness.server.commandTexts());
        harness.tickAt(30_000);
        assertEquals(List.of(30_000L), harness.timeline.timesOf("cmd kick {PLAYER}"));
    }

    @Test
    void e2InGameActionsRunBeforeCommandsOfTheSameStage() {
        Harness harness = new Harness(ladder(step(1, command("kick {PLAYER}", 0), trick(TrickType.SHUFFLE_INVENTORY, 0))));
        harness.flag(1);
        assertEquals(List.of("0 shuffle", "0 cmd kick {PLAYER}"), harness.timeline.rendered());
    }

    @Test
    void e3StagesFollowEachOthersLongestTimedEffect() {
        LadderStep step = step(1, dealt(30, 10, 0), taken(20, 25, 1), command("ban {PLAYER}", 2));
        assertEquals(List.of(0L, 10_000L, 35_000L), starts(step, 0));
        Harness harness = new Harness(ladder(step));
        harness.flag(1);
        serverTicks(harness, 50, 40_000);
        assertEquals(List.of(0L, 10_000L), harness.timeline.timesOf("effects"));
        assertEquals(List.of(35_000L), harness.timeline.timesOf("cmd"));
    }

    @Test
    void e4StagesWithoutTimedEffectsStartTogetherInOrder() {
        LadderStep step = step(1, trick(TrickType.SHUFFLE_INVENTORY, 0), trick(TrickType.LAUNCH, 1),
                dealt(40, 5, 2), command("kick {PLAYER}", 3));
        assertEquals(List.of(0L, 0L, 0L, 5_000L), starts(step, 0));
        Harness harness = new Harness(ladder(step));
        harness.flag(1);
        serverTicks(harness, 50, 6_000);
        assertEquals(List.of("0 shuffle", "0 launch", "0 effects " + Harness.STEVE, "5000 cmd kick {PLAYER}"),
                harness.timeline.rendered());
    }

    @Test
    void e5EmptyStageNumbersAreSkipped() {
        LadderStep step = step(1, dealt(30, 10, 0), lag(LagLevel.LIGHT, 20, 3), command("kick {PLAYER}", 5));
        assertEquals(List.of(0L, 10_000L, 30_000L), starts(step, 0));
    }

    @Test
    void e6LaterStageRunsOnTheFirstTickAtOrAfterItsStart() {
        LadderStep step = step(1, dealt(30, 30, 0), command("kick {PLAYER}", 1));
        assertEquals(List.of(1_000L, 31_000L), starts(step, 1_000));
        Harness harness = new Harness(ladder(step));
        harness.at(1_000);
        harness.flag(1);
        harness.tickAt(30_990);
        assertEquals(List.of(), harness.server.commandTexts());
        harness.tickAt(31_040);
        assertEquals(List.of(31_040L), harness.timeline.timesOf("cmd"));
    }

    @Test
    void e7SoftTemplateEffectsEndIndependently() {
        Harness harness = new Harness(ladder(step(1, dealt(70, 120, 0), taken(50, 90, 0))));
        harness.flag(1);
        harness.at(89_999);
        assertEquals(70, harness.dealt());
        assertEquals(50, harness.taken());
        harness.at(90_000);
        assertEquals(70, harness.dealt());
        assertEquals(0, harness.taken());
        harness.at(119_999);
        assertEquals(70, harness.dealt());
        harness.at(120_000);
        assertEquals(0, harness.dealt());
    }
}
