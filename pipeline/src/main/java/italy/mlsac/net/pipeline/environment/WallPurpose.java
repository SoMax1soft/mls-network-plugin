// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

public enum WallPurpose {
    SESSION_START("session.start"),
    SESSION_SAVED("session.saved"),
    RESTORE_START("restore.start"),
    RESTORE_SAVED("restore.saved");

    private final String tag;

    WallPurpose(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
