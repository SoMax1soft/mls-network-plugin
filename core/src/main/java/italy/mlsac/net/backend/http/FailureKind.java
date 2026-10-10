// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

public enum FailureKind {
    CONNECT(true),
    TIMEOUT(true),
    IO(true),
    CANCELLED(false),
    OTHER(false);

    private final boolean unreachable;

    FailureKind(boolean unreachable) {
        this.unreachable = unreachable;
    }

    public boolean unreachable() {
        return unreachable;
    }
}
