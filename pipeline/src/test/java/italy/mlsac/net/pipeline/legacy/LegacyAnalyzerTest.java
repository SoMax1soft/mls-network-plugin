// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LegacyAnalyzerTest {

    @Test
    void firstCallIsZeroThenFloatDifferencesAndModeAfterSixteenDivisors() {
        LegacyRotationAnalyzer analyzer = new LegacyRotationAnalyzer();
        assertEquals(LegacyDelta.ZERO, analyzer.analyze(0f, 3f));
        float previous = 0f;
        for (int k = 1; k <= 30; k++) {
            float yaw = k * 0.15f;
            LegacyDelta delta = analyzer.analyze(yaw, 3f);
            assertEquals(yaw - previous, delta.yawTurn());
            assertEquals(0f, delta.pitchTurn());
            previous = yaw;
            if (k < 16) {
                assertEquals(0.0, analyzer.yawStepEstimate());
            } else {
                assertEquals(0.15, analyzer.yawStepEstimate(), 1e-3);
            }
        }
    }

    @Test
    void resetMakesTheNextElementZero() {
        LegacyRotationAnalyzer analyzer = new LegacyRotationAnalyzer();
        analyzer.analyze(1f, 1f);
        analyzer.analyze(2f, 1.5f);
        analyzer.reset();
        assertEquals(LegacyDelta.ZERO, analyzer.analyze(40f, 2f));
    }

    @Test
    void nonFiniteInputChangesNothing() {
        LegacyRotationAnalyzer analyzer = new LegacyRotationAnalyzer();
        analyzer.analyze(1f, 1f);
        assertEquals(LegacyDelta.ZERO, analyzer.analyze(Float.NaN, 1f));
        assertEquals(1f, analyzer.analyze(2f, 1f).yawTurn());
    }
}
