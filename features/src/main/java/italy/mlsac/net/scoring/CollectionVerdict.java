// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public enum CollectionVerdict {
    SCORED,
    DETECTION_OFF,
    BYPASS,
    BEDROCK,
    REGION_EXEMPT;

    public static CollectionVerdict of(CollectionFacts facts) {
        if (!facts.detectionEnabled()) {
            return DETECTION_OFF;
        }
        if (facts.bypass()) {
            return BYPASS;
        }
        if (!facts.kind().scored()) {
            return BEDROCK;
        }
        if (facts.zoneExcluded()) {
            return REGION_EXEMPT;
        }
        return SCORED;
    }

    public boolean collects() {
        return this == SCORED;
    }
}
