// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BufferFormulaTest {

    private static final ScoringRules DEFAULTS = ScoringRules.pluginDefaults();

    @ParameterizedTest
    @CsvSource({"0.25,-0.75", "0.0,-1.5", "0.5,0.0", "0.7,0.0", "0.8,0.0"})
    void decayVectors(double probability, double change) {
        assertEquals(10.0 + change, BufferFormula.next(10.0, probability, DEFAULTS), 1e-12);
    }

    @Test
    void decayNeverGoesBelowZero() {
        assertEquals(0.0, BufferFormula.next(0.3, 0.0, DEFAULTS));
    }

    @Test
    void growthUsesExactDoubleOrder() {
        assertEquals(0.0 + (1.0 - 0.8) * 45.0, BufferFormula.next(0.0, 1.0, DEFAULTS));
        assertEquals(8.999999999999998, BufferFormula.next(0.0, 1.0, DEFAULTS));
        assertEquals(10.0 + (0.81 - 0.8) * 45.0, BufferFormula.next(10.0, 0.81, DEFAULTS));
    }

    @Test
    void negativeProbabilityIsClampedToZeroForDecay() {
        assertEquals(8.5, BufferFormula.next(10.0, -0.1, DEFAULTS), 1e-12);
    }

    @Test
    void nonPositiveDecayThresholdDecaysTheFullAmount() {
        ScoringRules rules = new ScoringRules(0.8, 45, 1.5, 0.0, 40, 20, 0.33);
        assertEquals(10.0, BufferFormula.next(10.0, 0.5, rules));
        assertEquals(8.5, BufferFormula.next(10.0, -0.5, rules));
    }

    @Test
    void probabilityExactlyAtDecayThresholdLeavesBufferUnchanged() {
        assertEquals(10.0, BufferFormula.next(10.0, 0.5, DEFAULTS));
    }
}
