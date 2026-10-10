// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

public enum UuidPurpose {
    SESSION_ID("session.id"),
    SESSION_RETRY("session.retry");

    private final String tag;

    UuidPurpose(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
