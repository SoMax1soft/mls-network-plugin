// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

public record AssistReadout(double roughSelfYaw, double jerkRatioYaw, double controlCorrYaw, double pullCorrYaw,
        double smoothPersonalYaw, double edgeRelease) {

    static final int SIGNALS = 6;

    static AssistReadout of(double roughSelfYaw, double jerkRatioYaw, double controlCorrYaw, double pullCorrYaw,
            double smoothPersonalYaw, double edgeRelease) {
        return new AssistReadout(finiteOrNaN(roughSelfYaw), finiteOrNaN(jerkRatioYaw), finiteOrNaN(controlCorrYaw),
                finiteOrNaN(pullCorrYaw), finiteOrNaN(smoothPersonalYaw), finiteOrNaN(edgeRelease));
    }


    void copyInto(double[] target, int offset) {
        target[offset] = roughSelfYaw;
        target[offset + 1] = jerkRatioYaw;
        target[offset + 2] = controlCorrYaw;
        target[offset + 3] = pullCorrYaw;
        target[offset + 4] = smoothPersonalYaw;
        target[offset + 5] = edgeRelease;
    }

    private static double finiteOrNaN(double signal) {
        return Double.isInfinite(signal) ? Double.NaN : signal;
    }
}
