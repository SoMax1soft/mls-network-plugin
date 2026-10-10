// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.RequestIdentity;
import italy.mlsac.net.backend.request.WindowSubmission;
import java.util.List;
import java.util.Optional;

public record PlainWire() implements UltraWire {

    private static final int VERSION = 4;

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public PredictRequest request(BackendBodies bodies, RequestIdentity identity, Optional<String> session,
                                  WindowSubmission submission) {
        return new PredictRequest(VERSION, bodies.predict(identity, session, UltraPayload.narrowed(submission)),
                List.of());
    }
}
