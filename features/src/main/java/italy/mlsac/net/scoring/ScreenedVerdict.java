// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.Objects;

public record ScreenedVerdict(String model, double probability, VerdictWeight weight, String displayName,
                              VerdictOrigin origin) {

    public ScreenedVerdict {
        Objects.requireNonNull(origin, "origin");
    }

    public ScreenedVerdict(String model, double probability, VerdictWeight weight, String displayName) {
        this(model, probability, weight, displayName, VerdictOrigin.MODEL_ANSWER);
    }

    public boolean onlyAlert() {
        return weight == VerdictWeight.ALERT_ONLY;
    }
}
