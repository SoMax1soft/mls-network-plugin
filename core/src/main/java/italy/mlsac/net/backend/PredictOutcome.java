// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend;

import java.util.Objects;
import java.util.Optional;

public sealed interface PredictOutcome
        permits PredictOutcome.Completed, PredictOutcome.Refused, PredictOutcome.Dropped, PredictOutcome.Failed {

    record Completed(int verdicts) implements PredictOutcome {
    }

    record Refused(RefusalReason reason) implements PredictOutcome {
    }

    record Dropped() implements PredictOutcome {
    }

    record Failed(String reason, Optional<String> declineCode) implements PredictOutcome {

        public Failed {
            Objects.requireNonNull(declineCode, "declineCode");
        }

        public Failed(String reason) {
            this(reason, Optional.empty());
        }

        public static Failed declined(String reason, String code) {
            return new Failed(reason, Optional.of(code));
        }

        public boolean declinedByBackend() {
            return declineCode.isPresent();
        }

        @Override
        public String toString() {
            return "Failed[reason=" + reason + "]";
        }
    }

    enum RefusalReason {
        NOT_INITIALIZED,
        NOT_CONNECTED,
        STASIS,
        QUOTA,
        BACKEND_FAULT
    }
}
