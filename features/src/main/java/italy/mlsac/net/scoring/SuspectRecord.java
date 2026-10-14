// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.DoubleUnaryOperator;

public final class SuspectRecord {

    static final int SHORT_HISTORY = 10;
    static final int RECENT_CHECKS = 50;
    static final int MODEL_HISTORY = 20;
    static final int PER_MODEL_HISTORY = 10;
    static final double HIGH_PROBABILITY = 0.8;

    private final UUID id;
    private final SuspectKind kind;
    private final ThresholdLatches latches = new ThresholdLatches();
    private final RollingWindow shortHistory = new RollingWindow(SHORT_HISTORY);
    private final RollingWindow lastScores = new RollingWindow(RECENT_CHECKS);
    private final ArrayDeque<ModelResult> modelHistory = new ArrayDeque<>(MODEL_HISTORY);
    private final Map<String, Double> lastByModel = new LinkedHashMap<>();
    private final Map<String, RollingWindow> historyByModel = new LinkedHashMap<>();
    private String name;
    private double latestScore;
    private int highProbabilityCount;
    private double buffer;
    private Trend trend = Trend.FALLING;
    private long historyRevision;

    public SuspectRecord(UUID id, String name, SuspectKind kind) {
        this.id = id;
        this.name = name;
        this.kind = kind;
    }

    public UUID id() {
        return id;
    }

    public synchronized String name() {
        return name;
    }

    public synchronized void rename(String newName) {
        this.name = newName;
    }

    public SuspectKind kind() {
        return kind;
    }

    public ThresholdLatches latches() {
        return latches;
    }

    public synchronized double buffer() {
        return buffer;
    }

    public synchronized double count(String model, double probability, DoubleUnaryOperator bufferUpdate) {
        double before = buffer;
        buffer = bufferUpdate.applyAsDouble(before);
        trend = Trend.after(trend, before, buffer);
        remember(model, probability);
        return buffer;
    }

    public synchronized double moveBuffer(DoubleUnaryOperator bufferUpdate) {
        buffer = bufferUpdate.applyAsDouble(buffer);
        return buffer;
    }

    public synchronized void resetBuffer(double value) {
        buffer = value;
    }

    public synchronized long historyRevision() {
        return historyRevision;
    }

    public synchronized int highProbabilityCount() {
        return highProbabilityCount;
    }

    public synchronized boolean hasChecks() {
        return !lastScores.oldestFirst().isEmpty();
    }

    public synchronized ModelHistory modelHistory() {
        return new ModelHistory(historyRevision, List.copyOf(modelHistory));
    }

    public synchronized SuspectSnapshot snapshot() {
        Map<String, List<Double>> perModel = new LinkedHashMap<>();
        historyByModel.forEach((model, window) -> perModel.put(model, window.oldestFirst()));
        return new SuspectSnapshot(id, name, latestScore, shortHistory.oldestFirst(), lastScores.oldestFirst(),
                List.copyOf(modelHistory), lastByModel, perModel, highProbabilityCount, buffer, trend,
                historyRevision);
    }

    private void remember(String model, double probability) {
        String modelId = ModelResult.normaliseModel(model);
        latestScore = probability;
        shortHistory.add(probability);
        lastScores.add(probability);
        if (modelHistory.size() == MODEL_HISTORY) {
            modelHistory.removeFirst();
        }
        modelHistory.addLast(new ModelResult(modelId, probability));
        lastByModel.put(modelId, probability);
        historyByModel.computeIfAbsent(modelId, key -> new RollingWindow(PER_MODEL_HISTORY)).add(probability);
        if (probability > HIGH_PROBABILITY) {
            highProbabilityCount++;
        }
        historyRevision++;
    }
}
