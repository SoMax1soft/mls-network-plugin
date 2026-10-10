// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend;

public sealed interface PredictOutcome
        permits PredictOutcome.Completed, PredictOutcome.Refused, PredictOutcome.Dropped, PredictOutcome.Failed {

    record Completed(int verdicts) implements PredictOutcome {
    }

    record Refused(RefusalReason reason) implements PredictOutcome {
    }

    record Dropped() implements PredictOutcome {
    }

    record Failed(String reason) implements PredictOutcome {
    }

    enum RefusalReason {
        NOT_INITIALIZED,
        NOT_CONNECTED,
        STASIS,
        QUOTA,
        BACKEND_FAULT
    }
}
