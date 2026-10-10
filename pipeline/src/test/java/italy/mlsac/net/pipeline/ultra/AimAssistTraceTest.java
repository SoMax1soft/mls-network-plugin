// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AimAssistTraceTest {

    @Test
    void personalSignalUsesTheFarMeanTakenBeforeThisRow() {
        AimAssistTrace trace = new AimAssistTrace();
        List<Double> deltas = new ArrayList<>();
        double previous = 0.0;
        List<Double> farDiffs = new ArrayList<>();
        List<Double> distantMagnitude = new ArrayList<>();
        List<Double> nearDiffs = new ArrayList<>();
        List<Double> closeMagnitude = new ArrayList<>();
        for (int row = 0; row < 25; row++) {
            double delta = row % 2 == 0 ? 1.0 : 3.0;
            trace.observe(steady(delta, 3.0));
            if (row >= 1) {
                farDiffs.add(Math.abs(delta - previous));
                distantMagnitude.add(Math.abs(delta));
            }
            previous = delta;
            deltas.add(delta);
        }
        for (int row = 0; row < 25; row++) {
            double delta = row % 2 == 0 ? 0.5 : 0.25;
            trace.observe(steady(delta, 0.0));
            nearDiffs.add(Math.abs(delta - previous));
            closeMagnitude.add(Math.abs(delta));
            previous = delta;
        }
        double lastDelta = 40.0;
        AssistReadout snapshot = trace.observe(steady(lastDelta, 3.0));
        double nearRatio = mean(nearDiffs) / Math.max(mean(closeMagnitude), 0.1);
        double farRatio = mean(farDiffs) / Math.max(mean(distantMagnitude), 0.1);
        double expected = Math.max(-4, Math.min(4, Math.log((nearRatio + 0.05) / (farRatio + 0.05))));
        assertEquals(expected, snapshot.smoothPersonalYaw(), 1e-12);
        farDiffs.add(Math.abs(lastDelta - previous));
        distantMagnitude.add(lastDelta);
        double afterPush = Math.log((nearRatio + 0.05)
                / (mean(farDiffs) / Math.max(mean(distantMagnitude), 0.1) + 0.05));
        assertTrue(Math.abs(afterPush - snapshot.smoothPersonalYaw()) > 1e-6);
    }

    @Test
    void newStretchForgetsShortRingsButKeepsPersonalOnes() {
        AimAssistTrace trace = new AimAssistTrace();
        for (int row = 0; row < 30; row++) {
            trace.observe(steady(row % 2 == 0 ? 1.0 : 2.0, 0.0));
        }
        trace.forgetShortTerm();
        AssistReadout snapshot = trace.observe(steady(1.0, 0.0));
        assertTrue(Double.isNaN(snapshot.roughSelfYaw()));
        assertTrue(Double.isNaN(snapshot.edgeRelease()));
    }

    private static AssistReading steady(double delta, double error) {
        return new AssistReading(delta, 0.0, 0.0, 0.0, error, 0.0, true, true, true, false, 0.0, 0.0);
    }

    private static double mean(List<Double> values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }
}
