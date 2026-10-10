// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public enum Trend {
    RISING,
    FALLING;

    static Trend after(Trend previous, double before, double after) {
        if (after > before) {
            return RISING;
        }
        if (after < before) {
            return FALLING;
        }
        return previous;
    }
}
