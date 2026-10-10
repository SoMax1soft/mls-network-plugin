// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.handoff;

public enum PenaltyAuthority {
    LEGACY_LADDER,
    SUPPRESSION_ENGINE;

    public static PenaltyAuthority of(boolean presetHasSuppression) {
        return presetHasSuppression ? SUPPRESSION_ENGINE : LEGACY_LADDER;
    }
}
