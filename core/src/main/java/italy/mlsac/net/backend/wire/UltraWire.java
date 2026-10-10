// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.RequestIdentity;
import italy.mlsac.net.backend.request.WindowSubmission;
import java.util.Optional;

public sealed interface UltraWire permits PlainWire {

    UltraWire PLAIN = new PlainWire();

    int version();

    PredictRequest request(BackendBodies bodies, RequestIdentity identity, Optional<String> session,
                           WindowSubmission submission);
}
