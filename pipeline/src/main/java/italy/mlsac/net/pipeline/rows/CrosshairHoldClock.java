// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import java.util.Objects;
import java.util.UUID;

final class CrosshairHoldClock {
    private static final int SATURATION = 255;

    private UUID crosshairVictim;
    private long since;
    private int onTargetCount;
    private long onTargetSince;

    boolean update(UUID selected, long now, boolean mainEyeHit) {
        boolean changed = !Objects.equals(selected, crosshairVictim);
        if (changed) {
            crosshairVictim = selected;
            since = now;
            onTargetCount = 0;
        }
        if (mainEyeHit) {
            if (onTargetCount == 0) {
                onTargetSince = now;
            }
            onTargetCount = Math.min(SATURATION, onTargetCount + 1);
        } else {
            onTargetCount = 0;
        }
        return changed;
    }

    void forget() {
        crosshairVictim = null;
        onTargetCount = 0;
    }

    double lockedOnMillis(long now) {
        return crosshairVictim == null ? -1.0 : (now - since) / 1e6;
    }

    OnTarget onTarget(long now) {
        return new OnTarget(onTargetCount, onTargetCount > 0 ? (now - onTargetSince) / 1e6 : 0.0);
    }
}
