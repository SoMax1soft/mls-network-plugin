// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

final class SensitivityAxis {
    private static final double LARGEST_TURN = 5.0;
    private static final int QUORUM = 15;

    private final DivisorRule rule;
    private final StepFrequencyTable histogram = new StepFrequencyTable();
    private final DivisorPolisher polisher = new DivisorPolisher();
    private double acceptedMagnitude;
    private double step;
    private boolean known;

    SensitivityAxis(DivisorRule rule) {
        this.rule = rule;
    }

    void absorb(double magnitude, float resolution) {
        collectDivisor(magnitude, resolution);
        double coarse = coarseStep();
        if (coarse > 0 && rule.refines(coarse, resolution)) {
            polisher.offer(magnitude, coarse);
        }
        double polished = polisher.refined();
        if (polished > 0) {
            step = polished;
            known = true;
        }
    }

    int histogramSize() {
        return histogram.size();
    }

    double step() {
        return step;
    }

    boolean known() {
        return known;
    }

    private void collectDivisor(double magnitude, float resolution) {
        if (!(magnitude > 0 && magnitude < LARGEST_TURN)) {
            return;
        }
        double divisor = RotationGcd.of(magnitude, acceptedMagnitude);
        if (!(divisor > RotationGcd.SMALLEST_STEP) || !rule.accepts(divisor, resolution)) {
            return;
        }
        if (acceptedMagnitude != 0) {
            histogram.add(divisor, rule.bucketWidth(resolution));
        }
        acceptedMagnitude = magnitude;
    }

    private double coarseStep() {
        if (histogram.size() <= QUORUM) {
            return 0.0;
        }
        StepFrequencyTable.Peak peak = histogram.mostPopular();
        return peak.count() > QUORUM ? peak.key() : 0.0;
    }
}
