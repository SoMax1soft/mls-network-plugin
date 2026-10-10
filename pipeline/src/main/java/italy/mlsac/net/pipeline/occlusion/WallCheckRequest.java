// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.LookDirection;
import italy.mlsac.net.pipeline.geometry.RayCast;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.player.PlayerSample;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record WallCheckRequest(
        OcclusionKind kind,
        OcclusionCell cell,
        UUID world,
        List<Vec3> eyes,
        List<Vec3> crosshairEnds,
        Box surface,
        Box tight) {

    private static final double UPRIGHT_BODY_HEIGHT = 1.8;
    private static final double MAXIMUM_RAISE = 0.3;

    public static WallCheckRequest of(OcclusionKind kind, OcclusionCell cell, PlayerSample eye, PlayerSample previousEye,
            Box possible, Box tight, double yaw, double pitch) {
        List<Vec3> eyes = new ArrayList<>(2);
        eyes.add(eye.eye());
        if (previousEye != null) {
            eyes.add(previousEye.eye());
        }
        Box surface = possible != null ? possible : tight;
        double raise = Math.min(MAXIMUM_RAISE, UPRIGHT_BODY_HEIGHT - tight.sizeY());
        if (raise > 0) {
            surface = surface.raisedTop(raise);
        }
        Vec3 direction = LookDirection.of(yaw, pitch);
        List<Vec3> ends = new ArrayList<>(eyes.size());
        for (Vec3 point : eyes) {
            ends.add(RayCast.entryOrNull(surface, point, direction));
        }
        return new WallCheckRequest(kind, cell, eye.world(), eyes, ends, surface, tight);
    }
}
