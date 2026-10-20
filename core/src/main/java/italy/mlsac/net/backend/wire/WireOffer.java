// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import italy.mlsac.net.json.JsonObject;
import java.util.Optional;

public final class WireOffer {

    public static final int HIGHEST_VERSION = ScrambledWire.WIDE_VERSION;
    private static final String OFFER = "wire5";
    private static final String WIDE_OFFER = "wire6";
    private static final String SEED = "seed";

    private WireOffer() {
    }

    public static UltraWire from(Optional<JsonObject> initReply) {
        return seedOf(initReply, WIDE_OFFER).<UltraWire>map(seed -> new ScrambledWire(seed, ScrambledWire.WIDE_VERSION))
                .or(() -> seedOf(initReply, OFFER).map(ScrambledWire::new))
                .orElse(UltraWire.PLAIN);
    }

    private static Optional<WireSeed> seedOf(Optional<JsonObject> initReply, String name) {
        Optional<JsonObject> offer = initReply.flatMap(reply -> reply.object(name))
                .or(() -> initReply.flatMap(reply -> reply.object("data")).flatMap(data -> data.object(name)));
        return offer.flatMap(object -> object.string(SEED)).flatMap(WireSeed::parse);
    }
}
