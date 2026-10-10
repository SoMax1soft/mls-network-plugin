// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SuspectSnapshot(UUID id, String name, double latestScore, List<Double> shortHistory,
                              List<Double> lastScores, List<ModelResult> modelHistory,
                              Map<String, Double> lastByModel, Map<String, List<Double>> historyByModel,
                              int highProbabilityCount, double buffer, Trend trend, long historyRevision) {

    public SuspectSnapshot {
        modelHistory = List.copyOf(modelHistory);
        lastByModel = Map.copyOf(lastByModel);
        historyByModel = Map.copyOf(historyByModel);
        shortHistory = List.copyOf(shortHistory);
        lastScores = List.copyOf(lastScores);
    }

    public static double mean(List<Double> values) {
        return values.isEmpty() ? 0.0 : values.stream().reduce(0.0, Double::sum) / values.size();
    }

    public double average() {
        return mean(shortHistory);
    }
}
