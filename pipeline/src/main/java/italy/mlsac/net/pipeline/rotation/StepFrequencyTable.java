// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

import java.util.HashMap;
import java.util.Map;

public final class StepFrequencyTable {
    private static final int REMEMBERED = 80;

    private final Map<Double, int[]> tallies = new HashMap<>();
    private final double[] remembered = new double[REMEMBERED];
    private int firstRemembered;
    private int rememberedCount;

    public void add(double divisor, double tolerance) {
        if (rememberedCount == REMEMBERED) {
            release(remembered[firstRemembered]);
            firstRemembered = (firstRemembered + 1) % REMEMBERED;
            rememberedCount--;
        }
        double key = existingKeyNear(divisor, tolerance);
        if (Double.isNaN(key)) {
            key = divisor;
            tallies.put(key, new int[1]);
        }
        tallies.get(key)[0]++;
        remembered[(firstRemembered + rememberedCount) % REMEMBERED] = key;
        rememberedCount++;
    }

    public int size() {
        return rememberedCount;
    }

    public Peak mostPopular() {
        double bestKey = Peak.NONE.key();
        int bestCount = Peak.NONE.count();
        for (Map.Entry<Double, int[]> tally : tallies.entrySet()) {
            int count = tally.getValue()[0];
            if (count > bestCount) {
                bestKey = tally.getKey();
                bestCount = count;
            }
        }
        return bestCount == Peak.NONE.count() ? Peak.NONE : new Peak(bestKey, bestCount);
    }

    public void clear() {
        tallies.clear();
        firstRemembered = 0;
        rememberedCount = 0;
    }

    private double existingKeyNear(double divisor, double tolerance) {
        for (Double key : tallies.keySet()) {
            if (Math.abs(key - divisor) < tolerance) {
                return key;
            }
        }
        return Double.NaN;
    }

    private void release(double key) {
        int[] tally = tallies.get(key);
        tally[0]--;
        if (tally[0] == 0) {
            tallies.remove(key);
        }
    }

    public record Peak(double key, int count) {
        static final Peak NONE = new Peak(0.0, 0);
    }
}
