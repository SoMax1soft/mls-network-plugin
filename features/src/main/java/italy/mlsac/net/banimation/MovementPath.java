// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.Optional;

public record MovementPath(int fromTick, int untilTick, double offsetX, double offsetY, double offsetZ,
                           boolean smooth) {

    public boolean covers(int tick) {
        return fromTick <= tick && tick <= untilTick;
    }

    public Optional<Point> target(int tick, Point start) {
        int moved = tick - fromTick;
        int duration = untilTick - fromTick;
        if (smooth && duration > 0) {
            double share = (double) moved / duration;
            return Optional.of(start.plus(offsetX * share, offsetY * share, offsetZ * share));
        }
        return moved == 0 ? Optional.of(start.plus(offsetX, offsetY, offsetZ)) : Optional.empty();
    }
}
