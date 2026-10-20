// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.RequestIdentity;
import italy.mlsac.net.backend.request.WindowSubmission;
import java.util.List;
import java.util.Optional;

public final class ScrambledWire implements UltraWire {

    public static final int VERSION = 5;
    public static final int WIDE_VERSION = 6;

    private final WireSeed seed;
    private final int version;
    private final int plainWidth;
    private final ScrambledWindows windows;

    public ScrambledWire(WireSeed seed) {
        this(seed, VERSION);
    }

    public ScrambledWire(WireSeed seed, int version) {
        if (version != VERSION && version != WIDE_VERSION) {
            throw new IllegalArgumentException("no scrambled wire version " + version);
        }
        this.seed = seed;
        this.version = version;
        this.plainWidth = version == WIDE_VERSION ? PlainWindow.WIDE_WIDTH : PlainWindow.WIDTH;
        this.windows = new ScrambledWindows(seed, plainWidth);
    }

    public WireSeed seed() {
        return seed;
    }

    @Override
    public int version() {
        return version;
    }

    @Override
    public PredictRequest request(BackendBodies bodies, RequestIdentity identity, Optional<String> session,
                                  WindowSubmission submission) {
        Optional<PlainWindow> window = PlainWindow.fromUltraPayload(submission.window(), plainWidth);
        if (window.isEmpty()) {
            return UltraWire.PLAIN.request(bodies, identity, session, submission);
        }
        byte[] json = bodies.predictScrambled(identity, session, submission, version,
                WireNumbers.flat(windows.scramble(window.get())));
        PackedBody packed = PackedBody.of(json, List.of());
        return new PredictRequest(version, packed.bytes(), packed.headers());
    }
}
