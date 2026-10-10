// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.wire.UltraWire;

public sealed interface OpenOutcome permits OpenOutcome.Opened, OpenOutcome.Rejected {

    record Opened(Endpoint endpoint, String sessionKey, UltraWire wire) implements OpenOutcome {
    }

    record Rejected(RejectionKind kind, String reason) implements OpenOutcome {
    }

    enum RejectionKind {
        AUTH,
        SWITCHABLE,
        OTHER
    }
}
