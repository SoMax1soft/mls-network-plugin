// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

public final class MovementClamp {

    private MovementClamp() {
    }

    public static Point verticalOnly(Point from, Point to) {
        return new Point(from.x(), to.y(), from.z());
    }
}
