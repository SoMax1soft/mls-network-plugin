// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

public enum MonotonicPurpose {
    STREAM_GATE("stream.gate"),
    STREAM_START("stream.start"),
    TRANSACTION("transaction"),
    SESSION_START("session.start");

    private final String tag;

    MonotonicPurpose(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
