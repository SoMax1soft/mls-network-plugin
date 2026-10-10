// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import java.util.List;

public record SuppressionPlan(FlagLadder ladder, BufferRules bufferRules, Capacity capacity, TrickTunings tricks,
                              ConsoleEcho consoleEcho) {

    private static final double DEFAULT_VIOLATION_THRESHOLD = 40.0;

    public static SuppressionPlan idle() {
        return new SuppressionPlan(new FlagLadder(LadderRepeat.STOP_AFTER_LAST, List.of()),
                BufferRules.defaults(BufferFiring.DISABLED), Capacity.ofThreshold(DEFAULT_VIOLATION_THRESHOLD),
                TrickTunings.defaults(), ConsoleEcho.SILENT);
    }

    public double thresholdOf(BufferRulePlan rule) {
        return capacity.thresholdFor(rule.percent());
    }
}
