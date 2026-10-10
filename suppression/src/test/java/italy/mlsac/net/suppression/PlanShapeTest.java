// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Plans.animated;
import static italy.mlsac.net.suppression.fixture.Plans.command;
import static italy.mlsac.net.suppression.fixture.Plans.dealt;
import static italy.mlsac.net.suppression.fixture.Plans.lag;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.plan.Capacity;
import italy.mlsac.net.suppression.plan.LagAction;
import italy.mlsac.net.suppression.plan.StagePlan;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanShapeTest {

    @Test
    void c6LagEffectsDefaultAndCanonicalOrder() {
        assertEquals(List.of(LagKind.DELAY, LagKind.MISS), lag(LagLevel.LIGHT, 5, 0).kinds());
        LagAction mixed = lag(LagLevel.LIGHT, 5, 0, LagKind.ROTATE, LagKind.DELAY, LagKind.ROTATE);
        assertEquals(List.of(LagKind.DELAY, LagKind.ROTATE), mixed.kinds());
    }

    @Test
    void c8CapacityAndRuleThresholds() {
        assertEquals(30.0, Capacity.ofThreshold(20).value());
        assertEquals(15.0, Capacity.ofThreshold(20).thresholdFor(50));
        assertEquals(11.25, Capacity.ofThreshold(45).thresholdFor(25));
        assertEquals(1.65, Capacity.ofThreshold(33).thresholdFor(5), 1e-12);
    }

    @Test
    void stagesGroupByValueKeepListOrderAndSplitCommands() {
        List<StagePlan> stages = step(1, command("kick a", 3), dealt(10, 5, 0), command("say b", 0), dealt(20, 7, 0))
                .stages();
        assertEquals(2, stages.size());
        assertEquals(List.of(dealt(10, 5, 0), dealt(20, 7, 0)), stages.get(0).inGame());
        assertEquals(List.of(command("say b", 0)), stages.get(0).commands());
        assertEquals(7_000L, stages.get(0).lastsMillis());
        assertEquals(0L, stages.get(1).lastsMillis());
    }

    @Test
    void k4AnimatedCommandCarriesThePrefix() {
        assertEquals("{ANIMATION} ban {PLAYER} Cheating", animated("ban {PLAYER} Cheating", 0).ladderText());
        assertEquals("ban {PLAYER}", command("ban {PLAYER}", 0).ladderText());
        assertEquals("cmd ban", animated("ban {PLAYER} Cheating", 0).token());
    }

    @Test
    void actionTokens() {
        assertEquals("-50% dmg 30s", dealt(50, 30, 0).token());
        assertEquals("lag medium [delay,miss] 20s", lag(LagLevel.MEDIUM, 20, 0, LagKind.MISS, LagKind.DELAY).token());
    }
}
