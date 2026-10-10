// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public record ReportingPolicy(boolean eventsEnabled, double alertThreshold) {

    public static final double BUILT_IN_ALERT_THRESHOLD = 0.75;

    public boolean reports(double probability) {
        return eventsEnabled && probability >= alertThreshold;
    }
}
