// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import italy.mlsac.net.json.JsonObject;
import java.util.Optional;

public final class WireOffer {

    public static final int HIGHEST_VERSION = UltraWire.PLAIN.version();

    private WireOffer() {
    }

    public static UltraWire from(Optional<JsonObject> initReply) {
        return UltraWire.PLAIN;
    }
}
