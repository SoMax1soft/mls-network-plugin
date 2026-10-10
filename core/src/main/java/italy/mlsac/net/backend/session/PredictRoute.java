// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.wire.UltraWire;
import java.util.Optional;

public record PredictRoute(Optional<String> sessionKey, UltraWire wire) {
}
