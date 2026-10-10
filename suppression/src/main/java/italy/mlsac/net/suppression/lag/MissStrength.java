// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

public record MissStrength(double chance, int consecutiveCap) {

    private static final int NO_CAP = Integer.MAX_VALUE;

    public static MissStrength capped(double chance, int consecutiveCap) {
        return new MissStrength(chance, consecutiveCap);
    }

    public static MissStrength uncapped(double chance) {
        return new MissStrength(chance, NO_CAP);
    }

    public boolean hasCap() {
        return consecutiveCap != NO_CAP;
    }

    public boolean capReachedAfter(int consecutiveMisses) {
        return hasCap() && consecutiveMisses >= consecutiveCap;
    }
}
