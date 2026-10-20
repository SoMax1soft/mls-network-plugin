// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.api.FallbackReason;
import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.api.ModelSelection;
import italy.mlsac.net.backend.BackendVerdict;
import italy.mlsac.net.backend.PredictOutcome;
import italy.mlsac.net.detection.ModelVerdict;
import italy.mlsac.net.testkit.FakeBackend;
import italy.mlsac.net.testkit.TestCore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServedModelWatchTest {

    private static final UUID PLAYER = new UUID(4, 4);
    private static final byte[] ULTRA_WINDOW = {'M', 'L', 'S', 'U', 4};
    private static final byte[] LEGACY_WINDOW = {1, 2, 3, 4};

    private final TestCore core = new TestCore();
    private final WindowDispatch dispatch = new WindowDispatch(core.backend, core.bus, player -> "Aura_00", core.log,
            core.workload, () -> 0L, new ServedModelWatch(core.selections, core.selections));

    @Test
    void anUltraWindowTheBackendDeclinesSendsTheChoiceBackToTheServerDefault() {
        core.selections.set(PLAYER, ModelChoice.ULTRA);
        dispatch.accept(PLAYER, ULTRA_WINDOW);

        FakeBackend.Pending sent = core.backend.predictions().get(0);
        assertArrayEquals(ULTRA_WINDOW, sent.request().window());
        sent.outcome().complete(PredictOutcome.Failed.declined("predict HTTP 409", "ULTRA_DISABLED"));

        assertEquals(ModelSelection.fellBack(PLAYER, ModelChoice.ULTRA, FallbackReason.REFUSED_BY_BACKEND),
                core.selections.get(PLAYER));
        assertEquals(Optional.of(FallbackReason.REFUSED_BY_BACKEND),
                core.selections.unavailableBecause(ModelChoice.ULTRA));
    }

    @Test
    void otherFailuresKeepTheChoice() {
        core.selections.set(PLAYER, ModelChoice.ULTRA);
        dispatch.accept(PLAYER, ULTRA_WINDOW);
        core.backend.predictions().get(0).outcome().complete(new PredictOutcome.Failed("transport: TIMEOUT"));
        dispatch.accept(PLAYER, ULTRA_WINDOW);
        core.backend.predictions().get(1).outcome().complete(
                new PredictOutcome.Refused(PredictOutcome.RefusalReason.STASIS));

        assertEquals(ModelSelection.granted(PLAYER, ModelChoice.ULTRA), core.selections.get(PLAYER));
    }

    @Test
    void aDeclineForAServerDefaultPlayerChangesNothing() {
        dispatch.accept(PLAYER, ULTRA_WINDOW);
        core.backend.predictions().get(0).outcome().complete(
                PredictOutcome.Failed.declined("predict HTTP 409", "ULTRA_DISABLED"));

        assertTrue(core.selections.available().contains(ModelChoice.ULTRA));
    }

    @Test
    void aProChoiceTheLicenceDoesNotServeFallsBackAfterThreeAnswers() {
        List<ModelVerdict> verdicts = new ArrayList<>();
        core.bus.subscribe(ModelVerdict.class, verdicts::add);
        core.selections.set(PLAYER, ModelChoice.PRO);
        for (int answer = 0; answer < ServedModelWatch.ANSWERS_BEFORE_NOT_SERVED; answer++) {
            assertEquals(ModelChoice.PRO, core.selections.get(PLAYER).effective());
            dispatch.legacy().accept(PLAYER, LEGACY_WINDOW);
            FakeBackend.Pending sent = core.backend.predictions().get(answer);
            assertArrayEquals(LEGACY_WINDOW, sent.request().window());
            sent.answer(new BackendVerdict("mini", 0.2));
        }

        assertEquals(ModelSelection.fellBack(PLAYER, ModelChoice.PRO, FallbackReason.NOT_SERVED_BY_BACKEND),
                core.selections.get(PLAYER));
        assertEquals(ServedModelWatch.ANSWERS_BEFORE_NOT_SERVED, verdicts.size());
    }

    @Test
    void anAnswerWithTheChosenModelResetsTheCount() {
        core.selections.set(PLAYER, ModelChoice.MINI);
        String[] answers = {"pro", "pro", "mini", "pro", "pro"};
        for (int index = 0; index < answers.length; index++) {
            dispatch.legacy().accept(PLAYER, LEGACY_WINDOW);
            core.backend.predictions().get(index).answer(new BackendVerdict(answers[index], 0.4));
        }

        assertFalse(core.selections.get(PLAYER).fellBack());
    }
}
