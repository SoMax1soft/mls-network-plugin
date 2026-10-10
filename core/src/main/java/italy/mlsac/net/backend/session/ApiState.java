// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

public enum ApiState {
    NOT_INITIALIZED,
    STASIS,
    CONNECTED,
    RATE_LIMITED,
    BACKEND_FAULT,
    DISCONNECTED
}
