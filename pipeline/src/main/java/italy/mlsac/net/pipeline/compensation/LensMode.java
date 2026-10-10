// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

public enum LensMode {
    NONE(0),
    TRANSACTIONS(1),
    REWIND(2);

    private final int code;

    LensMode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
