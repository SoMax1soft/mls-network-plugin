// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.environment.WorldSightLines;
import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.player.PlayerPosture;
import italy.mlsac.net.pipeline.player.PlayerSample;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OcclusionProbeTest {
    private static final UUID WORLD = new UUID(2, 2);
    private static final Box TIGHT = Box.player(0.0, 64.0, 3.0, 0.6, 1.8);
    private static final Vec3 INNER_LOW = new Vec3(TIGHT.low().x() + 0.05, TIGHT.low().y() + 0.05,
            TIGHT.low().z() + 0.05);
    private static final Vec3 INNER_HIGH = new Vec3(TIGHT.high().x() - 0.05, TIGHT.high().y() - 0.05,
            TIGHT.high().z() - 0.05);
    private static final PlayerSample EYE = sampleAt(0.0, 64.0, 0.0);
    private static final PlayerSample EARLIER_EYE = sampleAt(0.2, 64.0, -0.1);

    @Test
    void provenOpenAirCountsEveryPointVisibleWithoutRays() {
        ScriptedSightLines rays = new ScriptedSightLines(RayVerdict.BLOCKED);
        rays.provenFrom.add(EYE.eye());
        assertEquals(1.0, new OcclusionProbe(rays).exposure(request()));
        assertTrue(rays.traced.isEmpty());
        assertEquals(List.of(EYE.eye()), rays.provenAsked);
        Box span = rays.spans.get(0);
        assertEquals(INNER_LOW, span.low());
        assertEquals(INNER_HIGH, span.high());
    }

    @Test
    void withoutProofEveryPointIsAskedInTheSpecifiedOrder() {
        ScriptedSightLines rays = new ScriptedSightLines(RayVerdict.CLEAR);
        assertEquals(1.0, new OcclusionProbe(rays).exposure(request()));
        assertEquals(9, rays.traced.size());
        assertEquals(TIGHT.centre(), rays.traced.get(0));
        assertEquals(INNER_LOW, rays.traced.get(1));
        assertEquals(INNER_HIGH, rays.traced.get(8));
    }

    @Test
    void proofForTheEarlierEyeStillFollowsTheFirstEyesRays() {
        ScriptedSightLines rays = new ScriptedSightLines(RayVerdict.BLOCKED);
        rays.provenFrom.add(EARLIER_EYE.eye());
        assertEquals(1.0, new OcclusionProbe(rays).exposure(request()));
        assertEquals(9, rays.traced.size());
        assertEquals(List.of(EYE.eye(), EARLIER_EYE.eye()), rays.provenAsked);
    }

    private static WallCheckRequest request() {
        return WallCheckRequest.of(OcclusionKind.TICK, new OcclusionCell(), EYE, EARLIER_EYE, TIGHT, TIGHT, 0f, 0f);
    }

    private static PlayerSample sampleAt(double x, double y, double z) {
        return new PlayerSample(new UUID(1, 1), WORLD, 1, 0L, x, y, z, 0f, 0f, 1.62, 1.8, 0.6, 0.0, 1f, true, false,
                PlayerPosture.NEUTRAL, "air", null, null, null);
    }

    private static final class ScriptedSightLines implements WorldSightLines {
        private final RayVerdict answer;
        private final Set<Vec3> provenFrom = new HashSet<>();
        private final List<Vec3> provenAsked = new ArrayList<>();
        private final List<Box> spans = new ArrayList<>();
        private final List<Vec3> traced = new ArrayList<>();

        ScriptedSightLines(RayVerdict answer) {
            this.answer = answer;
        }

        @Override
        public RayVerdict trace(UUID world, Vec3 from, Vec3 to) {
            traced.add(to);
            return answer;
        }

        @Override
        public boolean provenClear(UUID world, Vec3 from, Box ends) {
            provenAsked.add(from);
            spans.add(ends);
            return provenFrom.contains(from);
        }
    }
}
