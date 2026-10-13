// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

public record RestoreAnswer(RestoreOutcome outcome, int ticks, int hits, String folder) {

    public static RestoreAnswer of(RestoreOutcome outcome) {
        return new RestoreAnswer(outcome, 0, 0, "");
    }
}
