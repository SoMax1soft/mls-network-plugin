// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

public enum ClickKind {
    LEFT,
    SHIFT_LEFT,
    RIGHT,
    SHIFT_RIGHT,
    OTHER;

    public boolean opensChecks() {
        return this == RIGHT || this == SHIFT_RIGHT;
    }
}
