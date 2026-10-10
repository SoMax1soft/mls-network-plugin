// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

public final class DivisorPolisher {
    private static final int KEPT_PAIRS = 256;
    private static final int TRUSTED_TOTAL = 30;
    private static final double DRIFT_SHARE = 0.01;
    private static final double ROUNDING_SLACK = 0.2;
    private static final long FIRST_GUESS_LIMIT = 8;
    private static final long SETTLED_LIMIT = 60;

    private final double[] magnitudes = new double[KEPT_PAIRS + 1];
    private final long[] multiples = new long[KEPT_PAIRS + 1];
    private int head;
    private int kept;
    private double magnitudeTotal;
    private long multipleTotal;

    public void offer(double magnitude, double coarse) {
        if (magnitude <= 0 || coarse <= 0) {
            return;
        }
        double estimate = refined();
        if (estimate > 0 && Math.abs(coarse - estimate) > estimate * DRIFT_SHARE) {
            forgetAll();
            estimate = 0;
        }
        double unit = estimate > 0 ? estimate : coarse;
        double ratio = magnitude / unit;
        long multiple = Math.round(ratio);
        if (fits(ratio, multiple, estimate > 0 ? SETTLED_LIMIT : FIRST_GUESS_LIMIT)) {
            keep(magnitude, multiple);
        }
    }

    public double refined() {
        return multipleTotal >= TRUSTED_TOTAL ? magnitudeTotal / multipleTotal : 0.0;
    }

    private static boolean fits(double ratio, long multiple, long limit) {
        return multiple >= 1 && multiple <= limit && Math.abs(ratio - multiple) <= ROUNDING_SLACK;
    }

    private void keep(double magnitude, long multiple) {
        int slot = (head + kept) % magnitudes.length;
        magnitudes[slot] = magnitude;
        multiples[slot] = multiple;
        kept++;
        magnitudeTotal += magnitude;
        multipleTotal += multiple;
        if (kept > KEPT_PAIRS) {
            magnitudeTotal -= magnitudes[head];
            multipleTotal -= multiples[head];
            head = (head + 1) % magnitudes.length;
            kept--;
        }
    }

    private void forgetAll() {
        head = 0;
        kept = 0;
        magnitudeTotal = 0.0;
        multipleTotal = 0L;
    }
}
