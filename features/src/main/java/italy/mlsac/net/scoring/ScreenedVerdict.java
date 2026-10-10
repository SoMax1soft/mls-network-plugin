// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public record ScreenedVerdict(String model, double probability, VerdictWeight weight, String displayName) {

    public boolean onlyAlert() {
        return weight == VerdictWeight.ALERT_ONLY;
    }
}
