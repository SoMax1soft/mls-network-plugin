// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.Endpoint;
import java.util.Optional;

public record BackendStatus(ApiState state, Optional<String> currentBase, Endpoint endpoint, boolean connected,
                            boolean stasis) {

    public static BackendStatus notInitialized() {
        return new BackendStatus(ApiState.NOT_INITIALIZED, Optional.empty(), Endpoint.PRIMARY, false, false);
    }

    public boolean latencyProbeAllowed() {
        return connected && !stasis;
    }
}
