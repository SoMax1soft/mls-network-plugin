// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

import java.util.EnumMap;
import java.util.Map;

public final class SensitivityEstimator {
    private final Map<LookAxis, SensitivityAxis> axes = new EnumMap<>(LookAxis.class);

    public SensitivityEstimator() {
        axes.put(LookAxis.YAW, new SensitivityAxis(new YawDivisorRule()));
        axes.put(LookAxis.PITCH, new SensitivityAxis(new PitchDivisorRule()));
    }

    public void observe(double yawMagnitude, double pitchMagnitude, float yaw, float referenceYaw) {
        float resolution = Math.ulp(Math.max(Math.abs(referenceYaw), Math.abs(yaw)));
        axes.get(LookAxis.YAW).absorb(yawMagnitude, resolution);
        axes.get(LookAxis.PITCH).absorb(pitchMagnitude, resolution);
    }

    public double step(LookAxis axis) {
        return axes.get(axis).known() ? axes.get(axis).step() : 0.0;
    }

    public boolean known(LookAxis axis) {
        return axes.get(axis).known();
    }
}
