// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

public enum MoveKind {
    RELATIVE(true, true),
    RELATIVE_WITH_ROTATION(true, true),
    ROTATION_ONLY(true, false),
    TELEPORT(false, true),
    POSITION_SYNC(false, true);

    private final boolean relative;
    private final boolean carriesPosition;

    MoveKind(boolean relative, boolean carriesPosition) {
        this.relative = relative;
        this.carriesPosition = carriesPosition;
    }

    public boolean relative() {
        return relative;
    }

    public boolean carriesPosition() {
        return carriesPosition;
    }
}
