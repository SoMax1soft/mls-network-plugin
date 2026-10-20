// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.api.ModelChoice;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModelRouteTest {

    @Test
    void theServerDefaultFollowsUltraMode() {
        assertTrue(ModelRoute.SERVER_DEFAULT.runsUltra(true));
        assertFalse(ModelRoute.SERVER_DEFAULT.runsLegacy(true));
        assertFalse(ModelRoute.SERVER_DEFAULT.runsUltra(false));
        assertTrue(ModelRoute.SERVER_DEFAULT.runsLegacy(false));
        assertFalse(ModelRoute.SERVER_DEFAULT.overrides());
    }

    @Test
    void everyOverrideRunsExactlyOnePipelineInBothServerModes() {
        for (boolean ultraMode : new boolean[] {true, false}) {
            assertTrue(ModelRoute.ULTRA.runsUltra(ultraMode));
            assertFalse(ModelRoute.ULTRA.runsLegacy(ultraMode));
            for (ModelRoute legacy : new ModelRoute[] {ModelRoute.PRO, ModelRoute.MINI}) {
                assertTrue(legacy.runsLegacy(ultraMode));
                assertFalse(legacy.runsUltra(ultraMode));
                assertTrue(legacy.overrides());
            }
        }
    }

    @Test
    void verdictModelsBelongToOneChoice() {
        assertTrue(ModelRoute.PRO.answersWith("pro"));
        assertFalse(ModelRoute.PRO.answersWith("fast"));
        assertTrue(ModelRoute.MINI.answersWith("fast"));
        assertTrue(ModelRoute.MINI.answersWith(" Mini "));
        assertTrue(ModelRoute.MINI.answersWith("fast_mini"));
        assertTrue(ModelRoute.ULTRA.answersWith("ultra"));
        assertFalse(ModelRoute.ULTRA.answersWith("experimental"));
        assertFalse(ModelRoute.SERVER_DEFAULT.answersWith("pro"));
        assertEquals(Optional.of(ModelChoice.MINI), ModelRoute.answeringChoice("mini"));
        assertEquals(Optional.of(ModelChoice.ULTRA), ModelRoute.answeringChoice("ULTRA"));
        assertEquals(Optional.empty(), ModelRoute.answeringChoice("pulse"));
        assertEquals(Optional.empty(), ModelRoute.answeringChoice(null));
        for (ModelChoice choice : ModelChoice.values()) {
            assertEquals(choice, ModelRoute.of(choice).choice());
            assertEquals(Optional.of(choice), ModelChoice.parse(choice.id()));
        }
        assertEquals(Optional.empty(), ModelChoice.parse("ultra80"));
    }
}
