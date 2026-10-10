// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

final class ExposureMemory {
    private static final int REUSE_LIMIT = 20;
    private static final int AXES = 3;

    private UUID world;
    private double[] eyeCoordinates = new double[2 * AXES];
    private int eyeCount = -1;
    private Box tight;
    private double exposure;
    private int reuses;

    boolean reusable(WallCheckRequest job) {
        return eyeCount >= 0 && reuses < REUSE_LIMIT && world.equals(job.world()) && sameEyes(job.eyes())
                && tight.sameBounds(job.tight());
    }

    double reuse() {
        reuses++;
        return exposure;
    }

    void remember(WallCheckRequest job, double computed) {
        List<Vec3> eyes = job.eyes();
        if (eyeCoordinates.length < eyes.size() * AXES) {
            eyeCoordinates = Arrays.copyOf(eyeCoordinates, eyes.size() * AXES);
        }
        for (int index = 0; index < eyes.size(); index++) {
            Vec3 eye = eyes.get(index);
            eyeCoordinates[index * AXES] = eye.x();
            eyeCoordinates[index * AXES + 1] = eye.y();
            eyeCoordinates[index * AXES + 2] = eye.z();
        }
        world = job.world();
        eyeCount = eyes.size();
        tight = job.tight();
        exposure = computed;
        reuses = 0;
    }

    private boolean sameEyes(List<Vec3> candidate) {
        if (candidate.size() != eyeCount) {
            return false;
        }
        for (int index = 0; index < eyeCount; index++) {
            Vec3 other = candidate.get(index);
            if (eyeCoordinates[index * AXES] != other.x() || eyeCoordinates[index * AXES + 1] != other.y()
                    || eyeCoordinates[index * AXES + 2] != other.z()) {
                return false;
            }
        }
        return true;
    }
}
