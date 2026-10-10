// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Harness.STEVE;
import static italy.mlsac.net.suppression.fixture.Plans.command;
import static italy.mlsac.net.suppression.fixture.Plans.dealt;
import static italy.mlsac.net.suppression.fixture.Plans.ladder;
import static italy.mlsac.net.suppression.fixture.Plans.step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.fixture.Harness;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.route.ForcedPunishment;
import italy.mlsac.net.suppression.route.LegacyAlertResponses;
import italy.mlsac.net.suppression.route.LegacyPenaltyLadder;
import italy.mlsac.net.suppression.route.ResponseSwitch;
import italy.mlsac.net.suppression.trigger.AnswerKind;
import italy.mlsac.net.suppression.trigger.BufferAnswer;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import italy.mlsac.net.suppression.trigger.SuspectOrigin;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoutingTest {

    private final List<String> legacy = new ArrayList<>();

    private final LegacyPenaltyLadder ladder = new LegacyPenaltyLadder() {
        @Override
        public void runFlag(FlagEvent flag) {
            legacy.add("ladder " + flag.context().vl());
        }

        @Override
        public void runTopRung(UUID suspectId, String suspectName) {
            legacy.add("top " + suspectName);
        }
    };

    private final LegacyAlertResponses responses = new LegacyAlertResponses() {
        @Override
        public void onAnswer(BufferAnswer answer) {
            legacy.add("responses " + answer.context().buffer());
        }

        @Override
        public void dropActiveReductions() {
            legacy.add("drop reductions");
        }
    };

    private ResponseSwitch switchWith(Harness harness, Optional<SuppressionPlan> plan) {
        ResponseSwitch routes = new ResponseSwitch(harness.engine(), ladder, responses);
        routes.apply(plan, id -> OptionalDouble.of(0));
        return routes;
    }

    private static FlagEvent flag(Harness harness, int vl) {
        return new FlagEvent(harness.context(vl, 0.9, 20), SuspectOrigin.PLAYER);
    }

    private static BufferAnswer answer(Harness harness, double buffer) {
        return new BufferAnswer(harness.context(0, 0.9, buffer), AnswerKind.SCORING, SuspectOrigin.PLAYER);
    }

    @Test
    void c1WithoutSuppressionTheLegacySystemsRun() {
        Harness harness = new Harness(ladder(step(1, command("kick {PLAYER}", 0))));
        ResponseSwitch routes = switchWith(harness, Optional.empty());
        routes.onFlag(flag(harness, 1));
        routes.onAnswer(answer(harness, 12));
        assertFalse(routes.engineActive());
        assertEquals(List.of("ladder 1", "responses 12.0"), legacy);
        assertTrue(harness.server.commands().isEmpty());
    }

    @Test
    void c2AndK1WithSuppressionTheLegacyCopiesNeverRun() {
        Harness harness = new Harness(ladder());
        ResponseSwitch routes = switchWith(harness, Optional.of(ladder(step(1, command("kick {PLAYER}", 0)))));
        legacy.clear();
        routes.onFlag(flag(harness, 1));
        routes.onAnswer(answer(harness, 39));
        assertTrue(routes.engineActive());
        assertEquals(List.of(), legacy);
        assertEquals(List.of("kick {PLAYER}"), harness.server.commandTexts());
    }

    @Test
    void switchingModesDropsTheOtherSystemsState() {
        Harness harness = new Harness(ladder());
        ResponseSwitch routes = switchWith(harness, Optional.empty());
        routes.apply(Optional.of(ladder(step(1, dealt(40, 30, 0), command("say later", 1)))), id -> OptionalDouble.of(0));
        assertEquals(List.of("drop reductions"), legacy);
        routes.onFlag(flag(harness, 1));
        assertEquals(40, harness.dealt());
        routes.apply(Optional.empty(), id -> OptionalDouble.of(0));
        assertEquals(0, harness.dealt());
        assertEquals(0, harness.engine().pendingCommands());
        assertEquals(1, harness.server.warnings().size());
    }

    @Test
    void k5ForcedPunishmentAlwaysUsesTheCompiledTopRung() {
        Harness harness = new Harness(ladder());
        ResponseSwitch routes = switchWith(harness, Optional.of(ladder(step(1, dealt(40, 30, 0)))));
        legacy.clear();
        new ForcedPunishment(ladder).punish(STEVE, "Steve");
        assertTrue(routes.engineActive());
        assertEquals(List.of("top Steve"), legacy);
        assertEquals(0, harness.dealt());
        assertEquals(0, harness.engine().pendingCommands());
    }
}
