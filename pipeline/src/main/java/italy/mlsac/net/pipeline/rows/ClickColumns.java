// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.geometry.HitMeasurement;

public final class ClickColumns {
    private final ClickRow row;
    private final boolean valid;
    private final HitMeasurement hit;

    public ClickColumns(ClickRow row) {
        this.row = row;
        this.valid = row.measurement().valid();
        this.hit = row.measurement().hit();
    }

    public ClickRow row() {
        return row;
    }

    public boolean targetValid() {
        return valid;
    }

    public int compMode() {
        return row.measurement().lens().mode().code();
    }

    public double pingMillis() {
        return row.measurement().lens().ping();
    }

    public int compOutstanding() {
        return row.measurement().lens().outstanding();
    }

    public boolean crosshairOnBox() {
        return valid && hit.crosshairOnBox();
    }

    public boolean crosshairOnTightBox() {
        return valid && hit.strict();
    }

    public int eyeRaysLanded() {
        return valid ? hit.eyeRaysLanded() : 0;
    }

    public boolean mainEyeHit() {
        return valid && hit.mainEyeHit();
    }

    public double impactOffsetX() {
        return valid ? hit.hitRelative().x() : 0.0;
    }

    public double impactOffsetY() {
        return valid ? hit.hitRelative().y() : 0.0;
    }

    public double impactOffsetZ() {
        return valid ? hit.hitRelative().z() : 0.0;
    }

    public double nearMissGap() {
        return valid ? hit.nearMissGap() : 0.0;
    }

    public double offsetInHalfWidths() {
        return valid ? hit.offsetInHalfWidths() : 0.0;
    }

    public double yawOffsetScaled() {
        return valid ? hit.yawOffsetScaled() : 0.0;
    }

    public double pitchOffsetScaled() {
        return valid ? hit.pitchOffsetScaled() : 0.0;
    }

    public double inflation() {
        return valid ? hit.inflation() : 0.0;
    }

    public double targetDistance() {
        return valid ? hit.distance() : 0.0;
    }
}
