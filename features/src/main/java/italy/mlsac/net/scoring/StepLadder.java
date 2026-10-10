// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.ArrayList;
import java.util.List;

public final class StepLadder {

    private static final double UPPER_BOUND = 1.0 + 1e-9;

    private volatile Cached cached = new Cached(Double.NaN, List.of());

    public static List<AlertStep> build(double stepPercent) {
        List<AlertStep> steps = new ArrayList<>();
        double step = usable(stepPercent) ? stepPercent : ScoringRules.DEFAULT_STEP_PERCENT;
        for (double sum = step; sum < UPPER_BOUND; sum = sum + step) {
            steps.add(AlertStep.of(Math.min(sum, 1.0)));
        }
        return List.copyOf(steps);
    }

    public List<AlertStep> steps(double stepPercent) {
        Cached current = cached;
        if (Double.compare(current.stepPercent(), stepPercent) != 0) {
            current = new Cached(stepPercent, build(stepPercent));
            cached = current;
        }
        return current.steps();
    }

    private static boolean usable(double stepPercent) {
        return stepPercent > 0.0 && stepPercent <= 1.0;
    }

    private record Cached(double stepPercent, List<AlertStep> steps) {
    }
}
